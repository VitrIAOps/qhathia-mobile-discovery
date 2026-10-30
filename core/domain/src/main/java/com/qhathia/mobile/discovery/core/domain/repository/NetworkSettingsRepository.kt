package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.NetworkConnectivityState
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import kotlinx.coroutines.flow.Flow

/**
 * Port for managing the device's connectivity from the Network settings screen. Implemented in
 * `core:data` over the Android `WifiManager` / `ConnectivityManager`. Mobile data is only read: no
 * non-system app can change it. Because the OS forbids some actions to non-privileged apps on API
 * 29+ (toggling Wi-Fi, silent connect), the
 * implementation uses the supported path where one exists and otherwise returns an explicit
 * [Result.failure] so the contract stays truthful. A future device-owner build can supply a
 * privileged implementation without changing this port.
 */
interface NetworkSettingsRepository {

    /** Emits the current [NetworkConnectivityState] immediately and again whenever it changes. */
    fun observeConnectivity(): Flow<NetworkConnectivityState>

    /** Emits the list of accessible Wi-Fi networks, refreshing as scan results change. */
    fun observeWifiNetworks(): Flow<List<WifiNetwork>>

    /** Requests Wi-Fi be turned [enabled] on/off; failure when the OS does not permit it. */
    suspend fun setWifiEnabled(enabled: Boolean): Result<Unit>

    /**
     * Starts a Wi-Fi scan ("Buscar redes", or right after the location permission is granted). The
     * list from [observeWifiNetworks] refreshes with the latest results, even when the OS throttles it.
     */
    fun rescan()

    /**
     * Attempts to connect to [ssid], using [password] for a secured network or `null` for an open
     * one. Returns [Result.failure] when the connection could not be requested or is not permitted.
     */
    suspend fun connect(ssid: String, password: String?): Result<Unit>
}
