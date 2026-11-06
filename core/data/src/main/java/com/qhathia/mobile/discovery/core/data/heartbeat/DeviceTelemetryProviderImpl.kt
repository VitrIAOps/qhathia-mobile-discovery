package com.qhathia.mobile.discovery.core.data.heartbeat

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import com.qhathia.mobile.discovery.core.data.network.ConnectivityDataSource
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.repository.DeviceTelemetryProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DeviceTelemetryProvider] over platform APIs. Each field is read in isolation so one failing
 * source only drops that field; none needs a permission beyond the declared `ACCESS_NETWORK_STATE`.
 */
@Singleton
class DeviceTelemetryProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityDataSource: ConnectivityDataSource,
) : DeviceTelemetryProvider {

    override suspend fun current(): DeviceTelemetry = DeviceTelemetry(
        appVersion = appVersion(),
        batteryLevel = runCatching { batteryLevel() }.getOrNull(),
        networkStatus = runCatching { connectivityDataSource.observe().first().toNetworkStatus() }.getOrNull(),
        model = runCatching { formatDeviceModel(Build.MANUFACTURER, Build.MODEL) }.getOrNull(),
        uptimeSeconds = deviceUptimeSeconds(),
    )

    private fun appVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: UNKNOWN_VERSION

    private fun batteryLevel(): Int? {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        return sanitizeBatteryLevel(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
    }

    /**
     * Time since the device booted, as the backend defines `uptimeSeconds` (F-63). The process time
     * was always 0 on the first heartbeat, sent from `Application.onCreate`.
     */
    private fun deviceUptimeSeconds(): Long = SystemClock.elapsedRealtime() / MILLIS_PER_SECOND

    private companion object {
        // `appVersion` is required by the API; a placeholder keeps the heartbeat alive if it cannot be read.
        const val UNKNOWN_VERSION = "unknown"
        const val MILLIS_PER_SECOND = 1_000L
    }
}
