package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Body of `POST /api/v1/devices/me/commands/{commandId}/ack` (api-contract.md §23). `detail` is
 * omitted when `null` (the shared `Json` keeps `encodeDefaults = false`).
 */
@Serializable
data class DeviceCommandAckDto(
    val success: Boolean,
    val detail: String? = null,
)
