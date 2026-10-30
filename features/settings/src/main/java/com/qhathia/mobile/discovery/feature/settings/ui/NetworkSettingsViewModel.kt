package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.usecase.ConnectToWifiUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ExitKioskModeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveNetworkStateUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveWifiNetworksUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RescanWifiNetworksUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SetWifiEnabledUseCase
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Network settings screen. Observes connectivity and the accessible-network list, and
 * routes the toggle / select / connect / rescan actions through their use cases. The airplane-mode
 * guard is derived from observed state in one place: while airplane mode is active, selecting a
 * network is blocked. An OS-rejected Wi-Fi toggle surfaces as an "action not permitted" flag;
 * connection outcomes drive the progress and error state.
 *
 * Mobile data is only shown: "Abrir ajustes de datos" leaves kiosk mode for the moment (as "Salir del
 * modo kiosko") and asks the screen to open Android's mobile-data screen. The state is read again when
 * the screen comes back.
 */
@HiltViewModel
class NetworkSettingsViewModel @Inject constructor(
    private val observeNetworkState: ObserveNetworkStateUseCase,
    observeWifiNetworks: ObserveWifiNetworksUseCase,
    private val setWifiEnabled: SetWifiEnabledUseCase,
    private val connectToWifi: ConnectToWifiUseCase,
    private val rescanWifiNetworks: RescanWifiNetworksUseCase,
    private val exitKioskMode: ExitKioskModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkSettingsUiState())
    val uiState: StateFlow<NetworkSettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NetworkSettingsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<NetworkSettingsUiEffect> = _uiEffect.asSharedFlow()

    private var networkStateJob: Job? = null

    init {
        readNetworkState()
        viewModelScope.launch {
            observeWifiNetworks().collect { networks ->
                _uiState.update { it.copy(networks = networks.toImmutableList()) }
            }
        }
    }

    fun onEvent(event: NetworkSettingsUiEvent) {
        when (event) {
            is NetworkSettingsUiEvent.ToggleWifi -> toggleWifi(event.enabled)
            NetworkSettingsUiEvent.OpenMobileDataSettings -> openMobileDataSettings()
            is NetworkSettingsUiEvent.SelectNetwork -> selectNetwork(event.network)
            is NetworkSettingsUiEvent.WifiPasswordChanged ->
                _uiState.update { it.copy(wifiPasswordInput = event.value) }
            NetworkSettingsUiEvent.SubmitWifiPassword -> submitWifiPassword()
            NetworkSettingsUiEvent.DismissWifiDialog -> dismissWifiDialog()
            NetworkSettingsUiEvent.RefreshNetworks -> rescanWifiNetworks()
            is NetworkSettingsUiEvent.LocationPermissionResult ->
                onLocationPermissionResult(event.granted, event.canAskAgain)
            NetworkSettingsUiEvent.RequestLocationPermission ->
                viewModelScope.launch { _uiEffect.emit(NetworkSettingsUiEffect.RequestLocationPermission) }
            NetworkSettingsUiEvent.ScreenResumed -> readNetworkState()
            NetworkSettingsUiEvent.BackPressed ->
                viewModelScope.launch { _uiEffect.emit(NetworkSettingsUiEffect.NavigateBack) }
        }
    }

    private fun toggleWifi(enabled: Boolean) {
        _uiState.update { it.copy(actionNotPermitted = false) }
        viewModelScope.launch {
            setWifiEnabled(enabled).onFailure {
                _uiState.update { state -> state.copy(actionNotPermitted = true) }
            }
        }
    }

    /**
     * (Re)starts reading the network state. The source reads a fresh snapshot when collection starts,
     * which catches changes made in Android's settings that send no connectivity callback.
     */
    private fun readNetworkState() {
        networkStateJob?.cancel()
        networkStateJob = viewModelScope.launch {
            observeNetworkState().collect { state ->
                _uiState.update {
                    it.copy(
                        wifiEnabled = state.wifiEnabled,
                        mobileDataEnabled = state.mobileDataEnabled,
                        simAvailable = state.simAvailable,
                        airplaneModeActive = state.airplaneModeActive,
                    )
                }
            }
        }
    }

    private fun onLocationPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        val wasGranted = _uiState.value.locationPermissionGranted
        _uiState.update {
            it.copy(locationPermissionGranted = granted, locationPermissionBlocked = !granted && !canAskAgain)
        }
        // Scans need the permission: scan right away so the list is not empty the first time.
        if (granted && !wasGranted) rescanWifiNetworks()
    }

    private fun openMobileDataSettings() {
        if (!_uiState.value.simAvailable) return
        // Android refuses to open another app's screen while the task is pinned.
        exitKioskMode()
        viewModelScope.launch { _uiEffect.emit(NetworkSettingsUiEffect.OpenMobileDataSettings) }
    }

    private fun selectNetwork(network: com.qhathia.mobile.discovery.core.domain.model.WifiNetwork) {
        // Airplane-mode guard: no selection while airplane mode is active.
        if (_uiState.value.airplaneModeActive) return
        if (network.isSecured) {
            _uiState.update {
                it.copy(selectedSecuredNetwork = network, wifiPasswordInput = "", connectionError = false)
            }
        } else {
            connect(network.ssid, password = null)
        }
    }

    private fun submitWifiPassword() {
        val network = _uiState.value.selectedSecuredNetwork ?: return
        connect(network.ssid, _uiState.value.wifiPasswordInput)
    }

    private fun connect(ssid: String, password: String?) {
        _uiState.update { it.copy(isConnecting = true, connectionError = false) }
        viewModelScope.launch {
            connectToWifi(ssid, password)
                .onSuccess {
                    _uiState.update {
                        it.copy(isConnecting = false, selectedSecuredNetwork = null, wifiPasswordInput = "")
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isConnecting = false, connectionError = true) }
                }
        }
    }

    private fun dismissWifiDialog() {
        _uiState.update {
            it.copy(selectedSecuredNetwork = null, wifiPasswordInput = "", connectionError = false)
        }
    }
}
