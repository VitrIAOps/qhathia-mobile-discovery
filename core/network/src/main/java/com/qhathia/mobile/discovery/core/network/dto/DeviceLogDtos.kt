package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTOs for `POST /api/v1/devices/logs` (api-contract.md). Required fields have no default, because
 * the shared `Json` keeps `encodeDefaults = false` and would leave them out.
 */

/** Request body: 1 to 100 log entries. */
@Serializable
data class DeviceLogsRequestDto(
    val logs: List<DeviceLogEntryDto>,
)

/**
 * One log entry. [level] is `INFO`, `DEBUG`, `WARN`, `ERROR` or `FATAL`; [timestamp] is ISO-8601 UTC.
 * [context] holds numbers only, which is all the tablet reports today.
 */
@Serializable
data class DeviceLogEntryDto(
    val level: String,
    val code: String? = null,
    val message: String,
    val timestamp: String,
    val context: Map<String, Long>? = null,
)
