package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import kotlinx.coroutines.flow.Flow

/** Port for the DEVICE heartbeat (`POST /api/v1/devices/heartbeat`). */
interface DeviceHeartbeatRepository {

    /**
     * Sends one heartbeat with [telemetry] using the persisted DEVICE token. Fails without a request
     * when the device is not paired. On success the result is also published to [observeHeartbeats]
     * and its commands to [observePendingCommands].
     */
    suspend fun sendHeartbeat(telemetry: DeviceTelemetry): Result<HeartbeatResult>

    /** Successful heartbeat results; a new collector immediately receives the latest one. */
    fun observeHeartbeats(): Flow<HeartbeatResult>

    /** Delivered commands, each received exactly once by a single collector and never replayed. */
    fun observePendingCommands(): Flow<DeviceCommand>
}
