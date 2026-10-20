package com.qhathia.mobile.discovery.core.data.heartbeat

import com.qhathia.mobile.discovery.core.data.network.ConnectivitySnapshot
import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.model.DeviceNetworkStatus

/*
 * Pure telemetry helpers, kept free of Android types so they run in JVM unit tests.
 */

/**
 * Builds the reported device model from `Build.MANUFACTURER` and `Build.MODEL`: avoids repeating the
 * manufacturer when the model already starts with it, capitalizes the first letter, and trims to
 * [DeviceConstants.MODEL_MAX_LENGTH]. Returns `null` when both are blank.
 */
internal fun formatDeviceModel(manufacturer: String?, model: String?): String? {
    val maker = manufacturer?.trim().orEmpty()
    val name = model?.trim().orEmpty()
    val combined = when {
        name.isEmpty() -> maker
        maker.isEmpty() || name.startsWith(maker, ignoreCase = true) -> name
        else -> "$maker $name"
    }
    if (combined.isBlank()) return null
    return combined
        .replaceFirstChar { it.uppercaseChar() }
        .take(DeviceConstants.MODEL_MAX_LENGTH)
}

/** Keeps a battery reading only when it is a valid percentage. */
internal fun sanitizeBatteryLevel(raw: Int?): Int? =
    raw?.takeIf { it in DeviceConstants.BATTERY_LEVEL_RANGE }

/** Maps a connectivity snapshot to the reported network transport (Wi-Fi wins over cellular). */
internal fun ConnectivitySnapshot.toNetworkStatus(): DeviceNetworkStatus = when {
    wifiActive || connectedSsid != null -> DeviceNetworkStatus.WIFI
    mobileDataActive -> DeviceNetworkStatus.MOBILE
    else -> DeviceNetworkStatus.OFFLINE
}
