package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/**
 * In-memory [DevicePairingRepository] for tests. Results are overridable per case; [emitStatus] pushes
 * pairing statuses to active collectors; [generateCount] and [exchangeCount] record the calls.
 */
class FakeDevicePairingRepository(
    var generateResult: Result<PairingCode> = Result.success(DEFAULT_CODE),
    var exchangeResult: Result<PairedDevice> = Result.success(DEFAULT_DEVICE),
    var pairedResult: Result<PairedDevice> = Result.success(DEFAULT_DEVICE),
    var unpairResult: Result<Unit> = Result.failure(UnsupportedOperationException("not available")),
) : DevicePairingRepository {

    private val statusFlow = MutableSharedFlow<PairingStatus>(extraBufferCapacity = 16)

    /** When set, [observeStatus] returns a flow that throws this on collection (e.g. a failed handshake). */
    var statusStreamError: Throwable? = null

    var generateCount: Int = 0
        private set

    /** Virtual time each [generateCode] call takes, to observe a request in flight. */
    var generateDelayMillis: Long = 0

    /** Number of [getPairedDevice] calls. */
    var pairedCount: Int = 0
        private set
    var lastObservedSession: String? = null
        private set
    var exchangeCount: Int = 0
        private set

    override suspend fun generateCode(): Result<PairingCode> {
        generateCount++
        if (generateDelayMillis > 0) delay(generateDelayMillis)
        return generateResult
    }

    var cancelAbandonedCount: Int = 0
        private set

    override suspend fun cancelAbandonedSession(): Result<Unit> {
        cancelAbandonedCount++
        return Result.success(Unit)
    }

    override fun observeStatus(sessionId: String): Flow<PairingStatus> {
        lastObservedSession = sessionId
        return statusStreamError?.let { error -> flow { throw error } } ?: statusFlow
    }

    /** Pushes [status] to current collectors of [observeStatus]. */
    suspend fun emitStatus(status: PairingStatus) = statusFlow.emit(status)

    override suspend fun exchangeForDeviceToken(sessionId: String): Result<PairedDevice> {
        exchangeCount++
        return exchangeResult
    }

    override suspend fun getPairedDevice(): Result<PairedDevice> {
        pairedCount++
        return pairedResult
    }

    override suspend fun unpair(): Result<Unit> = unpairResult

    companion object {
        val DEFAULT_CODE: PairingCode = PairingCode(
            code = "2Y567",
            sessionId = "sess-1",
            expiresAt = Instant.now().plusSeconds(300),
            ttlSeconds = 300,
        )
        val DEFAULT_DEVICE: PairedDevice = PairedDevice(
            deviceId = "dev-1",
            storeId = "store-1",
            deviceName = "Tablet Mostrador 1",
            platform = "ANDROID",
            status = "ACTIVE",
            pairingCode = "2Y567",
        )
    }
}
