package com.qhathia.mobile.discovery.core.data.network

import kotlinx.coroutines.flow.Flow

/**
 * Primitive connectivity reading taken from the platform, mapped by the repository to the domain
 * `NetworkConnectivityState`.
 *
 * @property wifiEnabled whether Wi-Fi is on.
 * @property mobileDataActive whether the cellular transport is the (or an) active network.
 * @property airplaneModeActive whether airplane mode is on.
 * @property connectedSsid the connected Wi-Fi SSID (quotes stripped), or `null`.
 * @property wifiActive whether Wi-Fi is the (or an) active network transport. Unlike [connectedSsid]
 *   it does not need the location permission, so it is the reliable "on Wi-Fi" signal.
 * @property hasInternet whether the active network (any transport) offers internet. Used to reconnect
 *   sockets as soon as the device is back online.
 */
data class ConnectivitySnapshot(
    val wifiEnabled: Boolean,
    val mobileDataActive: Boolean,
    val airplaneModeActive: Boolean,
    val connectedSsid: String?,
    val wifiActive: Boolean = false,
    val hasInternet: Boolean = false,
)

/**
 * Port over `ConnectivityManager` + airplane-mode global setting, exposed as a cold flow. Kept as an
 * interface so the repository mapping can be unit-tested with a fake.
 */
interface ConnectivityDataSource {

    /** Emits the current [ConnectivitySnapshot] and re-emits whenever connectivity changes. */
    fun observe(): Flow<ConnectivitySnapshot>
}
