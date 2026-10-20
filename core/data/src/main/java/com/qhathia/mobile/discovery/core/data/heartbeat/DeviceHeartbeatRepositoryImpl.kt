package com.qhathia.mobile.discovery.core.data.heartbeat

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.DeviceHeartbeatApiService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DeviceHeartbeatRepository] over [DeviceHeartbeatApiService]. Successful results are replayed to
 * new collectors (so a late subscriber still sees the latest `settingsVersion`), while each pending
 * command goes through a channel so it is consumed exactly once and never replayed — a replayed
 * `RESTART_APP` would restart the app again.
 */
@Singleton
class DeviceHeartbeatRepositoryImpl @Inject constructor(
    private val apiService: DeviceHeartbeatApiService,
    private val storeSessionRepository: StoreSessionRepository,
) : DeviceHeartbeatRepository {

    private val heartbeats = MutableSharedFlow<HeartbeatResult>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // Bounded: until a command consumer exists, undelivered commands must not grow without limit.
    private val commands = Channel<DeviceCommand>(
        capacity = COMMAND_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override suspend fun sendHeartbeat(telemetry: DeviceTelemetry): Result<HeartbeatResult> {
        val token = storeSessionRepository.deviceAccessToken()
        if (token.isNullOrBlank()) return Result.failure(IllegalStateException("Device is not paired"))

        return when (val result = apiService.sendHeartbeat(token, telemetry.toRequestDto())) {
            is NetworkResult.Success -> {
                val heartbeat = result.data.toDomain()
                heartbeats.emit(heartbeat)
                heartbeat.pendingCommands.forEach { commands.trySend(it) }
                Result.success(heartbeat)
            }
            is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
    }

    override fun observeHeartbeats(): Flow<HeartbeatResult> = heartbeats.asSharedFlow()

    override fun observePendingCommands(): Flow<DeviceCommand> = commands.receiveAsFlow()

    private companion object {
        const val COMMAND_BUFFER_CAPACITY = 16
    }
}
