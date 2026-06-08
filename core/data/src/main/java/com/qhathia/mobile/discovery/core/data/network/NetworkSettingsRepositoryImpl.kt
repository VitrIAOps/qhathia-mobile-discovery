package com.qhathia.mobile.discovery.core.data.network

import com.qhathia.mobile.discovery.core.domain.model.NetworkConnectivityState
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [NetworkSettingsRepository] mapping the Android data sources to domain models. The accessible-network
 * list is the Wi-Fi scan combined with the live connectivity snapshot so each entry can be flagged as
 * connected. Mobile-data toggling has no supported API for non-privileged apps, so it always returns a
 * clear [Result.failure]; Wi-Fi enable/connect delegate to the supported [WifiDataSource] paths.
 */
class NetworkSettingsRepositoryImpl @Inject constructor(
    private val wifiDataSource: WifiDataSource,
    private val connectivityDataSource: ConnectivityDataSource,
) : NetworkSettingsRepository {

    override fun observeConnectivity(): Flow<NetworkConnectivityState> =
        connectivityDataSource.observe().map { it.toDomain() }

    override fun observeWifiNetworks(): Flow<List<WifiNetwork>> =
        combine(
            wifiDataSource.observeScanResults(),
            connectivityDataSource.observe(),
        ) { scans, connectivity ->
            scans.map { it.toDomain(connectedSsid = connectivity.connectedSsid) }
        }

    override suspend fun setWifiEnabled(enabled: Boolean): Result<Unit> =
        wifiDataSource.setWifiEnabled(enabled)

    override suspend fun setMobileDataEnabled(enabled: Boolean): Result<Unit> =
        Result.failure(
            UnsupportedOperationException("Toggling mobile data is not permitted for non-privileged apps"),
        )

    override suspend fun connect(ssid: String, password: String?): Result<Unit> =
        wifiDataSource.connect(ssid, password)

    private fun ConnectivitySnapshot.toDomain(): NetworkConnectivityState = NetworkConnectivityState(
        wifiEnabled = wifiEnabled,
        mobileDataEnabled = mobileDataActive,
        airplaneModeActive = airplaneModeActive,
        connectedSsid = connectedSsid,
    )

    private fun WifiScanResult.toDomain(connectedSsid: String?): WifiNetwork = WifiNetwork(
        ssid = ssid,
        isSecured = secured,
        signalLevel = signalLevel,
        isConnected = connectedSsid != null && ssid == connectedSsid,
    )
}
