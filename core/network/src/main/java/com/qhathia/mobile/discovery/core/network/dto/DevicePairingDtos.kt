package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTOs for the tablet-side device pairing flow (api-contract.md §5). Only the fields the app reads
 * are declared; `ignoreUnknownKeys` drops the rest.
 */

/** Response of `POST /api/v1/devices/pairing-token`. */
@Serializable
data class PairingTokenResponseDto(
    val pairingToken: String,
    val sessionId: String,
    val expiresAt: String,
    // Relative TTL in seconds (`expiresAt − createdAt`, default 300). Drives the countdown so it is
    // immune to device↔server clock skew; nullable to tolerate older servers that only send expiresAt.
    val expiresInSeconds: Long? = null,
)

/**
 * Response of `GET /api/v1/devices/pairing-session/{sessionId}/token` (one-shot DEVICE JWT exchange).
 * [storeName] arrives since backend phase 37 (§37); optional so an older backend still parses.
 */
@Serializable
data class DeviceTokenResponseDto(
    val deviceId: String,
    val deviceAccessToken: String,
    val storeId: String,
    val deviceName: String,
    val platform: String,
    val status: String,
    val storeName: String? = null,
)

/**
 * Response of `GET /api/v1/devices/me`. [storeName] is not sent by the backend yet; the app reads it
 * when it arrives and keeps its placeholder while it is absent.
 */
@Serializable
data class DeviceInfoResponseDto(
    val deviceId: String,
    val storeId: String,
    val deviceName: String,
    val platform: String,
    val appVersion: String? = null,
    val status: String,
    val pairedAt: String? = null,
    val lastSeenAt: String? = null,
    val lastSyncAt: String? = null,
    val storeName: String? = null,
)

/** Status message pushed over the pairing-session WebSocket (`server → tablet`). */
@Serializable
data class PairingStatusMessageDto(
    val status: String,
    val sessionId: String? = null,
)

/**
 * Optional body of `POST /api/v1/devices/pairing-token` (api-contract.md §37): the session of the code
 * the tablet showed before, so the backend cancels it.
 */
@Serializable
data class GeneratePairingTokenRequestDto(
    val replacesSessionId: String,
)
