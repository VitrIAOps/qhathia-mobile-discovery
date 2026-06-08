package com.qhathia.mobile.discovery.core.domain.model

/**
 * An accessible Wi-Fi network discovered through the platform's standard scan APIs.
 *
 * @property ssid the network's name (SSID), used as the stable identity in lists.
 * @property isSecured whether the network requires a password (gates the Wi-Fi password dialog).
 * @property signalLevel a normalized signal strength bucket (0 = weakest), as reported by the platform.
 * @property isConnected whether the device is currently connected to this network.
 */
data class WifiNetwork(
    val ssid: String,
    val isSecured: Boolean,
    val signalLevel: Int,
    val isConnected: Boolean = false,
)
