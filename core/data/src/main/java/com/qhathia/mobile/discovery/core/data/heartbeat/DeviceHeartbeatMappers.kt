package com.qhathia.mobile.discovery.core.data.heartbeat

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatRequestDto
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatResponseDto
import com.qhathia.mobile.discovery.core.network.dto.PendingCommandDto

/** Maps the domain telemetry to the heartbeat request body. */
internal fun DeviceTelemetry.toRequestDto(): HeartbeatRequestDto = HeartbeatRequestDto(
    appVersion = appVersion,
    batteryLevel = batteryLevel,
    networkStatus = networkStatus?.name,
    model = model,
    uptimeSeconds = uptimeSeconds,
)

/** Maps the heartbeat response to the domain result. */
internal fun HeartbeatResponseDto.toDomain(): HeartbeatResult = HeartbeatResult(
    serverTime = serverTime,
    catalogVersion = catalogVersion,
    settingsVersion = settingsVersion,
    pendingCommands = pendingCommands.map { it.toDomain() },
)

internal fun PendingCommandDto.toDomain(): DeviceCommand = DeviceCommand(
    id = id,
    type = when (command.uppercase()) {
        COMMAND_REFRESH_CATALOG -> DeviceCommandType.RefreshCatalog
        COMMAND_RESTART_APP -> DeviceCommandType.RestartApp
        else -> DeviceCommandType.Unknown(command)
    },
)

private const val COMMAND_REFRESH_CATALOG = "REFRESH_CATALOG"
private const val COMMAND_RESTART_APP = "RESTART_APP"
