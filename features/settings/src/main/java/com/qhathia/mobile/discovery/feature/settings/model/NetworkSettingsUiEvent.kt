package com.qhathia.mobile.discovery.feature.settings.model

import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork

/** User-driven intents on the Network settings screen. */
sealed interface NetworkSettingsUiEvent {

    /** Toggle Wi-Fi on/off. */
    data class ToggleWifi(val enabled: Boolean) : NetworkSettingsUiEvent

    /** Toggle mobile data on/off. */
    data class ToggleMobileData(val enabled: Boolean) : NetworkSettingsUiEvent

    /** Select a network to connect to (secured networks open the password dialog). */
    data class SelectNetwork(val network: WifiNetwork) : NetworkSettingsUiEvent

    /** The Wi-Fi password field changed. */
    data class WifiPasswordChanged(val value: String) : NetworkSettingsUiEvent

    /** Submit the Wi-Fi password to connect to the selected secured network. */
    data object SubmitWifiPassword : NetworkSettingsUiEvent

    /** Close the Wi-Fi password dialog without connecting. */
    data object DismissWifiDialog : NetworkSettingsUiEvent

    /** Re-scan for available networks ("Buscar redes"). */
    data object RefreshNetworks : NetworkSettingsUiEvent

    /** The runtime location-permission request resolved with [granted]. */
    data class LocationPermissionResult(val granted: Boolean) : NetworkSettingsUiEvent

    /** Navigate back from the Network settings screen. */
    data object BackPressed : NetworkSettingsUiEvent
}
