package com.qhathia.mobile.discovery.core.data.heartbeat

import android.content.Context
import com.qhathia.mobile.discovery.core.device.connectivity.ConnectivityDataSource
import com.qhathia.mobile.discovery.core.device.management.DeviceInfoReader
import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.repository.DeviceTelemetryProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** [DeviceTelemetryProvider] over `core:device`'s device information and connectivity. */
@Singleton
class DeviceTelemetryProviderImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val connectivityDataSource: ConnectivityDataSource,
) : DeviceTelemetryProvider {

    private val infoReader = DeviceInfoReader(context, DeviceConstants.MODEL_MAX_LENGTH)

    override suspend fun current(): DeviceTelemetry {
        val info = infoReader.read()
        return DeviceTelemetry(
            appVersion = info.appVersion ?: UNKNOWN_VERSION,
            batteryLevel = info.batteryLevel,
            networkStatus = runCatching { connectivityDataSource.observe().first().toNetworkStatus() }.getOrNull(),
            model = info.model,
            uptimeSeconds = info.uptimeSeconds,
        )
    }

    private companion object {
        // `appVersion` is required by the API; a placeholder keeps the heartbeat alive if it cannot be read.
        const val UNKNOWN_VERSION = "unknown"
    }
}
