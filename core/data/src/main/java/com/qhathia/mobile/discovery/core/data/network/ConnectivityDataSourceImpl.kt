package com.qhathia.mobile.discovery.core.data.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ConnectivityDataSource] backed by `ConnectivityManager`, `WifiManager`, and the airplane-mode
 * global setting. Connectivity transitions arrive through a default-network callback; airplane-mode
 * changes through the `ACTION_AIRPLANE_MODE_CHANGED` broadcast. Each event re-reads a fresh snapshot.
 */
@Singleton
class ConnectivityDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityDataSource {

    private val connectivityManager: ConnectivityManager
        get() = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wifiManager: WifiManager
        get() = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // ACCESS_NETWORK_STATE is declared in :app; this library is only consumed there.
    @SuppressLint("MissingPermission")
    override fun observe(): Flow<ConnectivitySnapshot> = callbackFlow {
        fun pushCurrent() {
            trySend(snapshot())
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = pushCurrent()
            override fun onLost(network: Network) = pushCurrent()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = pushCurrent()
        }
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }

        val airplaneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = pushCurrent()
        }
        context.registerReceiver(
            airplaneReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
        )

        pushCurrent()

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
            runCatching { context.unregisterReceiver(airplaneReceiver) }
        }
    }

    // ACCESS_NETWORK_STATE is declared in :app; this library is only consumed there.
    @SuppressLint("MissingPermission")
    private fun snapshot(): ConnectivitySnapshot {
        val capabilities = runCatching {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        }.getOrNull()
        val onCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val onWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        return ConnectivitySnapshot(
            wifiEnabled = runCatching { wifiManager.isWifiEnabled }.getOrDefault(false),
            mobileDataActive = onCellular,
            airplaneModeActive = isAirplaneModeOn(),
            connectedSsid = if (onWifi) connectedSsid() else null,
            wifiActive = onWifi,
            hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        )
    }

    private fun isAirplaneModeOn(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    @SuppressLint("MissingPermission") // SSID read is best-effort; the screen gates location at point of use.
    @Suppress("DEPRECATION")
    private fun connectedSsid(): String? {
        val ssid = runCatching { wifiManager.connectionInfo?.ssid }.getOrNull()
            ?.removeSurrounding("\"")
        return ssid?.takeUnless { it.isBlank() || it == UNKNOWN_SSID }
    }

    private companion object {
        const val UNKNOWN_SSID = "<unknown ssid>"
    }
}
