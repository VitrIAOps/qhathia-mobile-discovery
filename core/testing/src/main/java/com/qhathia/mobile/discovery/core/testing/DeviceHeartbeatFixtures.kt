package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.model.DeviceNetworkStatus
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult

/** Domain fixtures for the device heartbeat. */
object DeviceHeartbeatFixtures {

    fun telemetry(
        appVersion: String = "1.4.0-alpha",
        batteryLevel: Int? = 85,
        networkStatus: DeviceNetworkStatus? = DeviceNetworkStatus.WIFI,
        model: String? = "Lenovo TB330FU",
        uptimeSeconds: Long? = 3_600,
    ): DeviceTelemetry = DeviceTelemetry(appVersion, batteryLevel, networkStatus, model, uptimeSeconds)

    fun result(
        settingsVersion: Long? = 4,
        catalogVersion: Long? = 1_713_200_000_000,
        pendingCommands: List<DeviceCommand> = emptyList(),
    ): HeartbeatResult = HeartbeatResult(
        serverTime = "2026-10-18T20:05:00Z",
        catalogVersion = catalogVersion,
        settingsVersion = settingsVersion,
        pendingCommands = pendingCommands,
    )

    fun refreshCommand(id: String = "c1"): DeviceCommand =
        DeviceCommand(id = id, type = DeviceCommandType.RefreshCatalog)
}
