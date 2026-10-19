package com.qhathia.mobile.discovery.core.domain.model

/**
 * Parsed response of a successful heartbeat.
 *
 * @property serverTime server clock (ISO-8601), or `null` when absent.
 * @property catalogVersion current catalog version, or `null` when absent.
 * @property settingsVersion version of this tablet's remote settings; a change means they must be
 *   re-read. `null` when the server does not send it.
 * @property pendingCommands commands delivered by this heartbeat (empty when none).
 */
data class HeartbeatResult(
    val serverTime: String? = null,
    val catalogVersion: Long? = null,
    val settingsVersion: Long? = null,
    val pendingCommands: List<DeviceCommand> = emptyList(),
)
