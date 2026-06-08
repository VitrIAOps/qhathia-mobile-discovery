package com.qhathia.mobile.discovery.core.data.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [WifiDataSource] backed by the Android `WifiManager`.
 *
 * Reading scan results requires `ACCESS_FINE_LOCATION` (requested at runtime by the Network settings
 * screen) and Wi-Fi enabled; `MissingPermission` is suppressed because the permission gate lives in
 * the UI. From API 29 the OS forbids non-privileged apps from toggling Wi-Fi and from silently
 * joining networks, so those paths use the supported suggestion API or return an explicit failure.
 */
@Singleton
class WifiDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WifiDataSource {

    private val wifiManager: WifiManager
        get() = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission") // ACCESS_FINE_LOCATION is requested at point of use in the UI.
    override fun observeScanResults(): Flow<List<WifiScanResult>> = callbackFlow {
        fun pushCurrent() {
            val results = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
            trySend(results.toScanResults())
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = pushCurrent()
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        // Emit whatever is already cached, then ask the platform for a fresh scan.
        pushCurrent()
        runCatching { @Suppress("DEPRECATION") wifiManager.startScan() }

        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    override suspend fun setWifiEnabled(enabled: Boolean): Result<Unit> =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            if (wifiManager.setWifiEnabled(enabled)) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Wi-Fi state change was rejected by the system"))
            }
        } else {
            Result.failure(
                UnsupportedOperationException("Toggling Wi-Fi is not permitted for apps on Android 10+"),
            )
        }

    @SuppressLint("MissingPermission") // CHANGE_WIFI_STATE is declared in :app; this library is only consumed there.
    override suspend fun connect(ssid: String, password: String?): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .apply { if (!password.isNullOrEmpty()) setWpa2Passphrase(password) }
                .build()
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            check(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                "Wi-Fi suggestion was not accepted (status=$status)"
            }
        } else {
            error("Direct Wi-Fi connection is not supported on this device")
        }
    }

    private fun List<ScanResult>.toScanResults(): List<WifiScanResult> =
        filter { it.ssidValue().isNotBlank() }
            .distinctBy { it.ssidValue() }
            .map { result ->
                WifiScanResult(
                    ssid = result.ssidValue(),
                    secured = result.capabilities.containsAnySecurity(),
                    signalLevel = signalLevelOf(result.level),
                )
            }

    @Suppress("DEPRECATION")
    private fun ScanResult.ssidValue(): String = (SSID ?: "").removeSurrounding("\"")

    private fun String.containsAnySecurity(): Boolean =
        SECURITY_MARKERS.any { contains(it) }

    private fun signalLevelOf(rssi: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wifiManager.calculateSignalLevel(rssi)
        } else {
            @Suppress("DEPRECATION")
            WifiManager.calculateSignalLevel(rssi, SIGNAL_LEVELS)
        }

    private companion object {
        val SECURITY_MARKERS = listOf("WEP", "WPA", "PSK", "EAP", "SAE")
        const val SIGNAL_LEVELS = 5
    }
}
