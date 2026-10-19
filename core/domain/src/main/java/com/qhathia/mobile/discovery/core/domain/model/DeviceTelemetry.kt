package com.qhathia.mobile.discovery.core.domain.model

/**
 * Operational state the tablet reports in each heartbeat. Every field except [appVersion] is
 * best-effort: `null` means "could not be read" and is omitted from the request.
 *
 * @property appVersion installed app version name (required by the API).
 * @property batteryLevel battery charge in percent (0–100).
 * @property networkStatus active network transport.
 * @property model manufacturer and model name, at most 100 characters.
 * @property uptimeSeconds seconds since the app process started.
 */
data class DeviceTelemetry(
    val appVersion: String,
    val batteryLevel: Int? = null,
    val networkStatus: DeviceNetworkStatus? = null,
    val model: String? = null,
    val uptimeSeconds: Long? = null,
)

/** Network transport reported in the heartbeat `networkStatus` field. */
enum class DeviceNetworkStatus { WIFI, MOBILE, OFFLINE }
