package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.NetworkConnectivityState
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [NetworkSettingsRepository] for ViewModel tests. The connectivity and network flows are
 * mutable so tests can drive state changes; the toggle/connect actions record their last argument and
 * (for the Wi-Fi toggle) reflect the change back into the connectivity flow, mimicking the platform.
 */
class FakeNetworkSettingsRepository(
    initialState: NetworkConnectivityState = NetworkConnectivityState.EMPTY,
    initialNetworks: List<WifiNetwork> = emptyList(),
) : NetworkSettingsRepository {

    val connectivity = MutableStateFlow(initialState)
    val networks = MutableStateFlow(initialNetworks)

    var setWifiResult: Result<Unit> = Result.success(Unit)
    var setMobileDataResult: Result<Unit> =
        Result.failure(UnsupportedOperationException("not permitted"))
    var connectResult: Result<Unit> = Result.success(Unit)

    var lastConnectedSsid: String? = null
        private set
    var lastConnectedPassword: String? = null
        private set

    override fun observeConnectivity(): Flow<NetworkConnectivityState> = connectivity.asStateFlow()

    override fun observeWifiNetworks(): Flow<List<WifiNetwork>> = networks.asStateFlow()

    override suspend fun setWifiEnabled(enabled: Boolean): Result<Unit> {
        if (setWifiResult.isSuccess) {
            connectivity.value = connectivity.value.copy(wifiEnabled = enabled)
        }
        return setWifiResult
    }

    override suspend fun setMobileDataEnabled(enabled: Boolean): Result<Unit> {
        if (setMobileDataResult.isSuccess) {
            connectivity.value = connectivity.value.copy(mobileDataEnabled = enabled)
        }
        return setMobileDataResult
    }

    override suspend fun connect(ssid: String, password: String?): Result<Unit> {
        lastConnectedSsid = ssid
        lastConnectedPassword = password
        if (connectResult.isSuccess) {
            connectivity.value = connectivity.value.copy(connectedSsid = ssid)
        }
        return connectResult
    }
}
