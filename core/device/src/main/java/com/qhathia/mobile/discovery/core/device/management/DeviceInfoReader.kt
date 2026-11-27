package com.qhathia.mobile.discovery.core.device.management

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock

/** What the device can say about itself. Each value is `null` when it cannot be read. */
data class DeviceInfo(
    val appVersion: String?,
    val batteryLevel: Int?,
    val model: String?,
    val uptimeSeconds: Long,
)

/** Reads [DeviceInfo] from Android. */
class DeviceInfoReader(
    private val context: Context,
    private val modelMaxLength: Int,
) {
    fun read(): DeviceInfo = DeviceInfo(
        appVersion = appVersion(),
        batteryLevel = runCatching { batteryLevel() }.getOrNull(),
        model = runCatching { formatDeviceModel(Build.MANUFACTURER, Build.MODEL, modelMaxLength) }.getOrNull(),
        uptimeSeconds = SystemClock.elapsedRealtime() / MILLIS_PER_SECOND,
    )

    private fun appVersion(): String? =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun batteryLevel(): Int? {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        return sanitizeBatteryLevel(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

/**
 * Builds the device model from `Build.MANUFACTURER` and `Build.MODEL`: avoids repeating the
 * manufacturer when the model already starts with it, capitalizes the first letter, and trims to
 * [maxLength]. Returns `null` when both are blank.
 */
fun formatDeviceModel(manufacturer: String?, model: String?, maxLength: Int = DEFAULT_MODEL_MAX_LENGTH): String? {
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
        .take(maxLength)
}

/** Keeps a battery reading only when it is a valid percentage. */
fun sanitizeBatteryLevel(raw: Int?): Int? = raw?.takeIf { it in BATTERY_LEVEL_RANGE }

private const val DEFAULT_MODEL_MAX_LENGTH = 100
private val BATTERY_LEVEL_RANGE = 0..100
