package com.qhathia.mobile.discovery.core.data.network

import kotlinx.coroutines.flow.Flow

/**
 * One discovered Wi-Fi access point, in primitive form. The repository maps this to the domain
 * `WifiNetwork`, adding the `isConnected` marker from the connectivity snapshot.
 *
 * @property ssid the network name (already stripped of surrounding quotes).
 * @property secured whether the network advertises any security (WPA/WPA2/WPA3/WEP/EAP).
 * @property signalLevel normalized signal bucket (0 = weakest).
 */
data class WifiScanResult(
    val ssid: String,
    val secured: Boolean,
    val signalLevel: Int,
)

/**
 * Port over the Android `WifiManager`: scan-result discovery plus the (OS-restricted) enable and
 * connect actions. Kept as an interface so the repository's mapping logic can be unit-tested with a
 * fake, without an Android framework dependency.
 */
interface WifiDataSource {

    /** Emits the currently discovered access points and re-emits as new scan results arrive. */
    fun observeScanResults(): Flow<List<WifiScanResult>>

    /** Turns Wi-Fi on/off where the OS still permits it (API < 29); otherwise a [Result.failure]. */
    suspend fun setWifiEnabled(enabled: Boolean): Result<Unit>

    /**
     * Requests a connection to [ssid] via the supported platform path (a `WifiNetworkSuggestion` on
     * API 29+, a legacy configuration below that), using [password] for a secured network.
     */
    suspend fun connect(ssid: String, password: String?): Result<Unit>
}
