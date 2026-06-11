package com.qhathia.mobile.discovery.core.data.pairing

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.DevicePairingApiService
import com.qhathia.mobile.discovery.core.network.service.PairingSocketClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DevicePairingRepository] over the pairing REST service and status WebSocket. On a successful token
 * exchange it persists the DEVICE token in the store session, so the rest of the app authenticates as
 * the paired device. The last generated code is remembered so the paired view can show "Código usado"
 * (the value is not returned by `/devices/me`).
 */
@Singleton
class DevicePairingRepositoryImpl @Inject constructor(
    private val apiService: DevicePairingApiService,
    private val socketClient: PairingSocketClient,
    private val storeSessionRepository: StoreSessionRepository,
) : DevicePairingRepository {

    @Volatile
    private var lastGeneratedCode: String? = null

    override suspend fun generateCode(): Result<PairingCode> =
        when (val result = apiService.generatePairingToken()) {
            is NetworkResult.Success -> {
                val code = result.data.toDomain()
                lastGeneratedCode = code.code
                Result.success(code)
            }
            is NetworkResult.HttpError -> Result.failure(httpError(result))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }

    override fun observeStatus(sessionId: String): Flow<PairingStatus> =
        socketClient.observe(sessionId).mapNotNull { it.toDomainOrNull() }

    override suspend fun exchangeForDeviceToken(sessionId: String): Result<PairedDevice> =
        when (val result = apiService.exchangeToken(sessionId)) {
            is NetworkResult.Success -> {
                val dto = result.data
                storeSessionRepository.saveDeviceSession(
                    deviceAccessToken = dto.deviceAccessToken,
                    deviceName = dto.deviceName,
                )
                Result.success(dto.toPairedDevice(lastGeneratedCode))
            }
            is NetworkResult.HttpError -> Result.failure(httpError(result))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }

    override suspend fun getPairedDevice(): Result<PairedDevice> {
        val token = storeSessionRepository.deviceAccessToken()
            ?: return Result.failure(IllegalStateException("Device is not paired"))
        return when (val result = apiService.getPairedDevice(token)) {
            is NetworkResult.Success -> Result.success(result.data.toPairedDevice(lastGeneratedCode))
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
}
