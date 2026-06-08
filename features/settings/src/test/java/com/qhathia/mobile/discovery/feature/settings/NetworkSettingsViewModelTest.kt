package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.NetworkConnectivityState
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.domain.usecase.ConnectToWifiUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveNetworkStateUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveWifiNetworksUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SetMobileDataEnabledUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SetWifiEnabledUseCase
import com.qhathia.mobile.discovery.core.testing.FakeNetworkSettingsRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.ui.NetworkSettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkSettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val securedNetwork = WifiNetwork(ssid = "Store", isSecured = true, signalLevel = 4)
    private val openNetwork = WifiNetwork(ssid = "Plaza", isSecured = false, signalLevel = 3)

    private fun viewModel(repository: FakeNetworkSettingsRepository) = NetworkSettingsViewModel(
        observeNetworkState = ObserveNetworkStateUseCase(repository),
        observeWifiNetworks = ObserveWifiNetworksUseCase(repository),
        setWifiEnabled = SetWifiEnabledUseCase(repository),
        setMobileDataEnabled = SetMobileDataEnabledUseCase(repository),
        connectToWifi = ConnectToWifiUseCase(repository),
    )

    @Test
    fun given_observed_state_then_ui_reflects_connectivity_and_networks() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository(
                initialState = NetworkConnectivityState(wifiEnabled = true),
                initialNetworks = listOf(securedNetwork, openNetwork),
            )
            val vm = viewModel(repository)

            val state = vm.uiState.value
            assertTrue(state.wifiEnabled)
            assertEquals(2, state.networks.size)
        }

    @Test
    fun given_wifi_toggled_on_then_state_reflects_enabled() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeNetworkSettingsRepository()
        val vm = viewModel(repository)

        vm.onEvent(NetworkSettingsUiEvent.ToggleWifi(true))

        assertTrue(vm.uiState.value.wifiEnabled)
    }

    @Test
    fun given_mobile_data_toggle_rejected_then_action_not_permitted() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository()
            // Default fake reproduces the platform restriction (mobile-data toggle returns failure).
            val vm = viewModel(repository)

            vm.onEvent(NetworkSettingsUiEvent.ToggleMobileData(true))

            assertTrue(vm.uiState.value.actionNotPermitted)
        }

    @Test
    fun given_airplane_mode_active_when_secured_network_selected_then_no_dialog() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository(
                initialState = NetworkConnectivityState(wifiEnabled = true, airplaneModeActive = true),
                initialNetworks = listOf(securedNetwork),
            )
            val vm = viewModel(repository)

            vm.onEvent(NetworkSettingsUiEvent.SelectNetwork(securedNetwork))

            assertFalse(vm.uiState.value.showWifiPasswordDialog)
            assertNull(vm.uiState.value.selectedSecuredNetwork)
        }

    @Test
    fun given_secured_network_selected_then_password_dialog_opens() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository(
                initialState = NetworkConnectivityState(wifiEnabled = true),
                initialNetworks = listOf(securedNetwork),
            )
            val vm = viewModel(repository)

            vm.onEvent(NetworkSettingsUiEvent.SelectNetwork(securedNetwork))

            assertTrue(vm.uiState.value.showWifiPasswordDialog)
            assertEquals(securedNetwork, vm.uiState.value.selectedSecuredNetwork)
        }

    @Test
    fun given_open_network_selected_then_connects_without_dialog() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository(
                initialState = NetworkConnectivityState(wifiEnabled = true),
                initialNetworks = listOf(openNetwork),
            )
            val vm = viewModel(repository)

            vm.onEvent(NetworkSettingsUiEvent.SelectNetwork(openNetwork))

            assertFalse(vm.uiState.value.showWifiPasswordDialog)
            assertEquals("Plaza", repository.lastConnectedSsid)
            assertNull(repository.lastConnectedPassword)
        }

    @Test
    fun given_password_submitted_then_connects_with_password_and_closes_dialog() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository(
                initialState = NetworkConnectivityState(wifiEnabled = true),
                initialNetworks = listOf(securedNetwork),
            )
            val vm = viewModel(repository)

            vm.onEvent(NetworkSettingsUiEvent.SelectNetwork(securedNetwork))
            vm.onEvent(NetworkSettingsUiEvent.WifiPasswordChanged("secret"))
            vm.onEvent(NetworkSettingsUiEvent.SubmitWifiPassword)

            assertEquals("Store", repository.lastConnectedSsid)
            assertEquals("secret", repository.lastConnectedPassword)
            assertFalse(vm.uiState.value.showWifiPasswordDialog)
        }

    @Test
    fun given_connection_fails_then_error_shown_and_dialog_kept() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeNetworkSettingsRepository(
                initialState = NetworkConnectivityState(wifiEnabled = true),
                initialNetworks = listOf(securedNetwork),
            ).apply { connectResult = Result.failure(IllegalStateException("bad password")) }
            val vm = viewModel(repository)

            vm.onEvent(NetworkSettingsUiEvent.SelectNetwork(securedNetwork))
            vm.onEvent(NetworkSettingsUiEvent.WifiPasswordChanged("nope"))
            vm.onEvent(NetworkSettingsUiEvent.SubmitWifiPassword)

            val state = vm.uiState.value
            assertTrue(state.connectionError)
            assertFalse(state.isConnecting)
            assertTrue(state.showWifiPasswordDialog)
        }

    @Test
    fun given_back_pressed_then_emits_navigate_back() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeNetworkSettingsRepository())

        vm.uiEffect.test {
            vm.onEvent(NetworkSettingsUiEvent.BackPressed)
            assertTrue(awaitItem() is NetworkSettingsUiEffect.NavigateBack)
        }
    }
}
