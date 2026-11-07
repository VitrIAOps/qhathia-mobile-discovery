package com.qhathia.mobile.discovery.core.data.pairing

import com.qhathia.mobile.discovery.core.data.network.ConnectivityDataSource
import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeError
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeException
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.DevicePairingApiService
import com.qhathia.mobile.discovery.core.network.service.PairingExchangeResponse
import com.qhathia.mobile.discovery.core.network.service.PairingHandshakeRejectedException
import com.qhathia.mobile.discovery.core.network.service.PairingSocketClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DevicePairingRepository] over the pairing REST service and status WebSocket (design D5).
 *
 * - [observeStatus] keeps the socket alive across drops: it reconnects with backoff (1, 2, 4, then 8 s)
 *   and right away when the device comes back online. The server sends the current status on every
 *   connect, so nothing pushed while offline is lost. It never polls the exchange endpoint.
 * - When the server refuses the handshake, the exchange is called once to learn why.
 * - [exchangeForDeviceToken] runs at most one successful exchange per session (the endpoint is
 *   one-shot): later calls get the cached device. Connectivity errors are retried; 410
 *   `ALREADY_USED` is not.
 *
 * On a successful exchange the DEVICE token is persisted in the store session, with the code that was
 * used, so the paired view can show "Código usado" after a restart (`/devices/me` does not return it).
 * When the exchange or `/devices/me` sends the store name, it is saved in the session for Home and
 * Settings (F-05: right after pairing, without opening Settings).
 *
 * The session of the code on screen is kept in [pendingSession] (F-02): each new code replaces it on
 * the backend, a successful exchange forgets it, and [cancelAbandonedSession] cancels the one a
 * previous process left behind.
 */
@Singleton
class DevicePairingRepositoryImpl @Inject constructor(
    private val apiService: DevicePairingApiService,
    private val socketClient: PairingSocketClient,
    private val storeSessionRepository: StoreSessionRepository,
    private val connectivityDataSource: ConnectivityDataSource,
    private val pendingSession: PendingPairingSessionStore,
) : DevicePairingRepository {

    @Volatile
    private var lastGeneratedCode: String? = null

    private val exchangeMutex = Mutex()
    private var exchanged: Pair<String, PairedDevice>? = null

    override suspend fun generateCode(): Result<PairingCode> =
        when (val result = apiService.generatePairingToken(replacesSessionId = pendingSession.get())) {
            is NetworkResult.Success -> {
                val code = result.data.toDomain()
                lastGeneratedCode = code.code
                pendingSession.set(code.sessionId)
                Result.success(code)
            }
            is NetworkResult.HttpError -> Result.failure(httpError(result))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }

    override suspend fun cancelAbandonedSession(): Result<Unit> {
        val pending = pendingSession.get() ?: return Result.success(Unit)
        if (!storeSessionRepository.deviceAccessToken().isNullOrBlank()) {
            pendingSession.clearIf(pending)
            return Result.success(Unit)
        }
        val result = apiService.cancelPairingSession(pending)
        // 404: unknown to the backend, so there is nothing left to cancel.
        val done = result is NetworkResult.Success ||
            (result is NetworkResult.HttpError && result.code == HTTP_NOT_FOUND)
        if (done) {
            pendingSession.clearIf(pending)
            return Result.success(Unit)
        }
        return when (result) {
            is NetworkResult.HttpError -> Result.failure(httpError(result))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
            is NetworkResult.Success -> Result.success(Unit)
        }
    }

    override fun observeStatus(sessionId: String): Flow<PairingStatus> = channelFlow {
        // Offline → online transitions, conflated: one pending signal is enough to skip the backoff.
        val backOnline = Channel<Unit>(Channel.CONFLATED)
        val watcher = launch {
            connectivityDataSource.observe()
                .map { it.hasInternet }
                .distinctUntilChanged()
                .drop(1)
                .filter { it }
                .collect { backOnline.trySend(Unit) }
        }
        try {
            var attempt = 0
            while (true) {
                backOnline.tryReceive() // a signal from before this attempt is stale
                val end = connectOnce(sessionId) { status ->
                    attempt = 0 // the socket works again
                    send(status)
                }
                when (end) {
                    SocketEnd.FinalStatus -> return@channelFlow
                    SocketEnd.Dropped -> Unit
                    SocketEnd.HandshakeRejected -> {
                        val final = fallbackExchange(sessionId)
                        if (final != null) {
                            send(final)
                            return@channelFlow
                        }
                    }
                }
                withTimeoutOrNull(backoffMillis(attempt)) { backOnline.receive() }
                attempt++
            }
        } finally {
            watcher.cancel()
        }
    }

    /** Collects one socket connection, forwarding statuses until the first final one. */
    private suspend fun connectOnce(
        sessionId: String,
        onStatus: suspend (PairingStatus) -> Unit,
    ): SocketEnd {
        var final = false
        return try {
            socketClient.observe(sessionId)
                .mapNotNull { it.toDomainOrNull() }
                .transformWhile { status ->
                    emit(status)
                    status == PairingStatus.Waiting
                }
                .collect { status ->
                    if (status != PairingStatus.Waiting) final = true
                    onStatus(status)
                }
            if (final) SocketEnd.FinalStatus else SocketEnd.Dropped
        } catch (cancel: CancellationException) {
            // Our own cancellation ends the loop; a cancellation from inside the socket is a drop.
            currentCoroutineContext().ensureActive()
            if (final) SocketEnd.FinalStatus else SocketEnd.Dropped
        } catch (_: PairingHandshakeRejectedException) {
            SocketEnd.HandshakeRejected
        } catch (_: Exception) {
            if (final) SocketEnd.FinalStatus else SocketEnd.Dropped
        }
    }

    /**
     * Asks the exchange endpoint why the handshake was refused. Returns the final status to emit, or
     * `null` to keep reconnecting. `Approved` means the exchange already succeeded: the caller's own
     * exchange then gets the cached device without a second request.
     */
    private suspend fun fallbackExchange(sessionId: String): PairingStatus? {
        val result = exchangeForDeviceToken(sessionId)
        if (result.isSuccess) return PairingStatus.Approved
        val error = (result.exceptionOrNull() as? PairingExchangeException)?.error
        return when (error) {
            PairingExchangeError.Rejected -> PairingStatus.Rejected
            PairingExchangeError.Expired, PairingExchangeError.NotFound -> PairingStatus.Expired
            PairingExchangeError.AlreadyUsed -> throw PairingExchangeException(error)
            PairingExchangeError.NotApproved,
            PairingExchangeError.Connectivity,
            PairingExchangeError.Other,
            null,
            -> null
        }
    }

    override suspend fun exchangeForDeviceToken(sessionId: String): Result<PairedDevice> =
        exchangeMutex.withLock {
            exchanged?.takeIf { it.first == sessionId }?.let { return@withLock Result.success(it.second) }
            exchangeWithRetry(sessionId).onSuccess { exchanged = sessionId to it }
        }

    private suspend fun exchangeWithRetry(sessionId: String): Result<PairedDevice> {
        var retries = 0
        while (true) {
            when (val response = apiService.exchangeToken(sessionId)) {
                is PairingExchangeResponse.Success -> {
                    val dto = response.data
                    storeSessionRepository.saveDeviceSession(
                        deviceAccessToken = dto.deviceAccessToken,
                        deviceName = dto.deviceName,
                        pairingCode = lastGeneratedCode,
                    )
                    dto.storeName?.takeIf { it.isNotBlank() }?.let { storeSessionRepository.updateStoreName(it) }
                    pendingSession.clearIf(sessionId)
                    return Result.success(dto.toPairedDevice(lastGeneratedCode))
                }
                is PairingExchangeResponse.HttpError ->
                    return Result.failure(PairingExchangeException(response.toExchangeError()))
                is PairingExchangeResponse.NetworkError -> when {
                    response.kind == NetworkErrorKind.Unknown ->
                        return Result.failure(PairingExchangeException(PairingExchangeError.Other))
                    retries >= MAX_EXCHANGE_RETRIES ->
                        return Result.failure(PairingExchangeException(PairingExchangeError.Connectivity))
                    else -> delay(backoffMillis(retries++))
                }
            }
        }
    }

    override suspend fun getPairedDevice(): Result<PairedDevice> {
        val token = storeSessionRepository.deviceAccessToken()
            ?: return Result.failure(IllegalStateException("Device is not paired"))
        return when (val result = apiService.getPairedDevice(token)) {
            is NetworkResult.Success -> {
                val dto = result.data
                val sentStoreName = dto.storeName?.takeIf { it.isNotBlank() }
                if (sentStoreName != null) storeSessionRepository.updateStoreName(sentStoreName)
                val session = storeSessionRepository.observe().first()
                Result.success(
                    dto.toPairedDevice(pairingCode = session.pairingCode, storeName = session.storeName),
                )
            }
            is NetworkResult.HttpError -> Result.failure(httpError(result))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
    }

    // TODO(pairing-unpair): no backend unpair API exists yet (ClickUp 86e1kbt2d). Fail explicitly so the
    // UI can communicate that server-side unpairing is unavailable, rather than clearing the local token
    // and falsely implying the device was unpaired on the server (which would strand the device).
    override suspend fun unpair(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Unpairing is not available yet"))

    private fun httpError(error: NetworkResult.HttpError): Throwable =
        IllegalStateException("HTTP ${error.code}: ${error.message}")

    private enum class SocketEnd { FinalStatus, Dropped, HandshakeRejected }

    internal companion object {
        /** Retries of the exchange after a connectivity error (5 attempts in about 15 s). */
        const val MAX_EXCHANGE_RETRIES = 4

        const val HTTP_NOT_FOUND = 404

        /** 1 s, 2 s, 4 s, then 8 s at most. */
        fun backoffMillis(attempt: Int): Long = 1_000L shl attempt.coerceIn(0, 3)
    }
}
