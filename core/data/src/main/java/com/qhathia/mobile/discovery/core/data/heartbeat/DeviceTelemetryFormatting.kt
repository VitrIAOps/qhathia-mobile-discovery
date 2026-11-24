package com.qhathia.mobile.discovery.core.data.heartbeat

import com.qhathia.mobile.discovery.core.device.connectivity.ConnectivitySnapshot
import com.qhathia.mobile.discovery.core.domain.model.DeviceNetworkStatus

/** Maps a connectivity snapshot to the reported network transport (Wi-Fi wins over cellular). */
internal fun ConnectivitySnapshot.toNetworkStatus(): DeviceNetworkStatus = when {
    wifiActive || connectedSsid != null -> DeviceNetworkStatus.WIFI
    mobileDataActive -> DeviceNetworkStatus.MOBILE
    else -> DeviceNetworkStatus.OFFLINE
}
