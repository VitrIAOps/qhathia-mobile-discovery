package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response of `GET /api/v1/devices/me/settings` (api-contract.md §21). Missing fields fall back to the
 * server defaults. `exitPinHash` is the BCrypt hash of the exit PIN, or `null` when none is set.
 */
@Serializable
data class DeviceSettingsDto(
    val idleShowcaseEnabled: Boolean = true,
    val voiceAssistantEnabled: Boolean = true,
    val brightnessMode: String = "AUTO",
    val kioskMode: Boolean = true,
    val exitPinHash: String? = null,
    val autoSyncEnabled: Boolean = true,
    val settingsVersion: Long? = null,
)
