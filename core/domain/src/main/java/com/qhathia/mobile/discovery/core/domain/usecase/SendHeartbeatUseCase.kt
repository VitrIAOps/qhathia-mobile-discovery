package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceTelemetryProvider
import javax.inject.Inject

/** Reads the current telemetry and sends one heartbeat with it. */
class SendHeartbeatUseCase @Inject constructor(
    private val telemetryProvider: DeviceTelemetryProvider,
    private val repository: DeviceHeartbeatRepository,
) {
    suspend operator fun invoke(): Result<HeartbeatResult> =
        repository.sendHeartbeat(telemetryProvider.current())
}
