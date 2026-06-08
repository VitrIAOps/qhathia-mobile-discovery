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
 */
data class ConnectivitySnapshot(
    val wifiEnabled: Boolean,
    val mobileDataActive: Boolean,
    val airplaneModeActive: Boolean,
    val connectedSsid: String?,
)

/**
 * Port over `ConnectivityManager` + airplane-mode global setting, exposed as a cold flow. Kept as an
 * interface so the repository mapping can be unit-tested with a fake.
 */
interface ConnectivityDataSource {

    /** Emits the current [ConnectivitySnapshot] and re-emits whenever connectivity changes. */
    fun observe(): Flow<ConnectivitySnapshot>
}
