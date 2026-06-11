package com.qhathia.mobile.discovery.core.domain.model

import java.time.Instant

/**
 * The device's paired identity, shown in the "Comercio vinculado" view. Sourced from the device-token
 * exchange and `GET /api/v1/devices/me`. Fields with no API source (e.g. [storeName]) are nullable and
 * surfaced as "pending" by the presentation layer rather than left blank.
 *
 * @property pairingCode the code used to pair, carried from generation (not returned by `/devices/me`).
 * @property storeName the merchant (store) display name — not provided by any API yet (pending).
 */
data class PairedDevice(
    val deviceId: String,
    val storeId: String,
    val deviceName: String,
    val platform: String,
    val status: String,
    val appVersion: String? = null,
    val lastSyncAt: Instant? = null,
    val pairingCode: String? = null,
    val storeName: String? = null,
)
