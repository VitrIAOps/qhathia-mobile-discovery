package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * In-memory [DeviceHeartbeatRepository] for tests. Records every telemetry sent in [sent] and answers
 * with [nextResult] (success by default). A successful answer is published like production: to the
 * replaying heartbeat stream and, command by command, to the once-only command stream.
 */
class FakeDeviceHeartbeatRepository(
    var nextResult: Result<HeartbeatResult> = Result.success(DeviceHeartbeatFixtures.result()),
) : DeviceHeartbeatRepository {

    val sent: MutableList<DeviceTelemetry> = mutableListOf()

    private val heartbeats = MutableSharedFlow<HeartbeatResult>(replay = 1)
    private val commands = Channel<DeviceCommand>(Channel.UNLIMITED)

    override suspend fun sendHeartbeat(telemetry: DeviceTelemetry): Result<HeartbeatResult> {
        sent += telemetry
        nextResult.onSuccess { result ->
            heartbeats.emit(result)
            result.pendingCommands.forEach { commands.send(it) }
        }
        return nextResult
    }

    override fun observeHeartbeats(): Flow<HeartbeatResult> = heartbeats.asSharedFlow()

    override fun observePendingCommands(): Flow<DeviceCommand> = commands.receiveAsFlow()
}
