package com.qhathia.mobile.discovery.core.data.network

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NetworkSettingsRepositoryImplTest {

    private fun snapshot(
        wifiEnabled: Boolean = true,
        mobileDataActive: Boolean = false,
        airplaneModeActive: Boolean = false,
        connectedSsid: String? = null,
        simAvailable: Boolean = false,
        mobileDataEnabled: Boolean = false,
    ) = ConnectivitySnapshot(
        wifiEnabled = wifiEnabled,
        mobileDataActive = mobileDataActive,
        airplaneModeActive = airplaneModeActive,
        connectedSsid = connectedSsid,
        simAvailable = simAvailable,
        mobileDataEnabled = mobileDataEnabled,
    )

    private class FakeWifiDataSource(
        private val scans: List<WifiScanResult>,
        var setWifiResult: Result<Unit> = Result.success(Unit),
        var connectResult: Result<Unit> = Result.success(Unit),
    ) : WifiDataSource {
        var rescans = 0
        override fun observeScanResults(): Flow<List<WifiScanResult>> = flowOf(scans)
        override fun rescan() {
            rescans++
        }
        override suspend fun setWifiEnabled(enabled: Boolean): Result<Unit> = setWifiResult
        override suspend fun connect(ssid: String, password: String?): Result<Unit> = connectResult
    }

    private class FakeConnectivityDataSource(
        private val snapshot: ConnectivitySnapshot,
    ) : ConnectivityDataSource {
        override fun observe(): Flow<ConnectivitySnapshot> = flowOf(snapshot)
    }

    @Test
    fun given_snapshot_when_observed_then_mapped_to_domain_state() = runTest {
        val repository = NetworkSettingsRepositoryImpl(
            wifiDataSource = FakeWifiDataSource(emptyList()),
            connectivityDataSource = FakeConnectivityDataSource(
                snapshot(
                    wifiEnabled = true,
                    airplaneModeActive = true,
                    connectedSsid = "Home",
                    simAvailable = true,
                    mobileDataEnabled = true,
                ),
            ),
        )

        repository.observeConnectivity().test {
            val state = awaitItem()
            assertTrue(state.wifiEnabled)
            assertTrue(state.mobileDataEnabled)
            assertTrue(state.simAvailable)
            assertTrue(state.airplaneModeActive)
            assertEquals("Home", state.connectedSsid)
            awaitComplete()
        }
    }

    @Test
    fun given_scan_results_when_observed_then_connected_network_is_marked() = runTest {
        val repository = NetworkSettingsRepositoryImpl(
            wifiDataSource = FakeWifiDataSource(
                listOf(
                    WifiScanResult(ssid = "Home", secured = true, signalLevel = 4),
                    WifiScanResult(ssid = "Cafe", secured = false, signalLevel = 2),
                ),
            ),
            connectivityDataSource = FakeConnectivityDataSource(snapshot(connectedSsid = "Home")),
        )

        repository.observeWifiNetworks().test {
            val networks = awaitItem()
            assertEquals(2, networks.size)
            val home = networks.first { it.ssid == "Home" }
            val cafe = networks.first { it.ssid == "Cafe" }
            assertTrue(home.isConnected)
            assertTrue(home.isSecured)
            assertFalse(cafe.isConnected)
            assertFalse(cafe.isSecured)
            awaitComplete()
        }
    }

    @Test
    fun given_data_setting_off_while_on_wifi_then_mobile_data_is_inactive() = runTest {
        // Wi-Fi carries the traffic; only the data setting says whether mobile data is on.
        val repository = NetworkSettingsRepositoryImpl(
            wifiDataSource = FakeWifiDataSource(emptyList()),
            connectivityDataSource = FakeConnectivityDataSource(snapshot(simAvailable = true, mobileDataEnabled = false)),
        )

        repository.observeConnectivity().test {
            val state = awaitItem()
            assertFalse(state.mobileDataEnabled)
            assertTrue(state.simAvailable)
            awaitComplete()
        }
    }

    @Test
    fun given_rescan_when_requested_then_delegates_to_data_source() {
        val wifi = FakeWifiDataSource(emptyList())
        val repository = NetworkSettingsRepositoryImpl(wifi, FakeConnectivityDataSource(snapshot()))

        repository.rescan()

        assertEquals(1, wifi.rescans)
    }

    @Test
    fun given_wifi_toggle_and_connect_when_requested_then_delegates_to_data_source() = runTest {
        val wifi = FakeWifiDataSource(
            scans = emptyList(),
            setWifiResult = Result.failure(IllegalStateException("denied")),
            connectResult = Result.success(Unit),
        )
        val repository = NetworkSettingsRepositoryImpl(wifi, FakeConnectivityDataSource(snapshot()))

        assertTrue(repository.setWifiEnabled(true).isFailure)
        assertTrue(repository.connect("Cafe", null).isSuccess)
    }
}
