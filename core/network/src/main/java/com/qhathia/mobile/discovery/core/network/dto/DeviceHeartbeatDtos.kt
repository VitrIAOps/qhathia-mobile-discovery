package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTOs for `POST /api/v1/devices/heartbeat` (api-contract.md §5, §18, §21, §23). Optional request
 * fields default to `null` and, because the shared `Json` keeps `encodeDefaults = false`, are left out
 * of the body when unknown. Response fields other than `received` are optional so an older backend
 * still parses.
 */

/** Request body: the telemetry the tablet reports on each heartbeat. */
@Serializable
data class HeartbeatRequestDto(
    val appVersion: String,
    val batteryLevel: Int? = null,
    val networkStatus: String? = null,
    val model: String? = null,
    val uptimeSeconds: Long? = null,
)

/** Response body of a heartbeat. */
@Serializable
data class HeartbeatResponseDto(
    val received: Boolean = true,
    val serverTime: String? = null,
    val catalogVersion: Long? = null,
    val settingsVersion: Long? = null,
    val pendingCommands: List<PendingCommandDto> = emptyList(),
)

/** A command delivered in the heartbeat response (`REFRESH_CATALOG`, `RESTART_APP`, …). */
@Serializable
data class PendingCommandDto(
    val id: String,
    val command: String,
)
