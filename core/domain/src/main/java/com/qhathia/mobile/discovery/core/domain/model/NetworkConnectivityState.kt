package com.qhathia.mobile.discovery.core.domain.model

/**
 * Snapshot of the device's connectivity, observed by the Network settings screen.
 *
 * @property wifiEnabled whether Wi-Fi is currently turned on.
 * @property mobileDataEnabled whether the mobile-data setting is on (read-only for the app).
 * @property simAvailable whether the tablet can use mobile data (telephony and a ready SIM).
 * @property airplaneModeActive whether airplane mode is active (blocks selecting/changing a network).
 * @property connectedSsid the SSID of the connected Wi-Fi network, or `null` when not connected.
 */
data class NetworkConnectivityState(
    val wifiEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false,
    val airplaneModeActive: Boolean = false,
    val connectedSsid: String? = null,
    val simAvailable: Boolean = false,
) {
    companion object {
        /** Well-defined default, emitted before the platform reports any real state. */
        val EMPTY: NetworkConnectivityState = NetworkConnectivityState()
    }
}
