package com.qhathia.mobile.discovery.feature.settings.model

import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork

/** User-driven intents on the Network settings screen. */
sealed interface NetworkSettingsUiEvent {

    /** Toggle Wi-Fi on/off. */
    data class ToggleWifi(val enabled: Boolean) : NetworkSettingsUiEvent

    /**
     * "Abrir ajustes de datos": leave kiosk mode for the moment and open Android's mobile-data screen
     * (the app cannot change mobile data itself). Ignored without a SIM.
     */
    data object OpenMobileDataSettings : NetworkSettingsUiEvent

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

    /**
     * The runtime location-permission request resolved with [granted]. [canAskAgain] is `false` when
     * Android no longer shows the request (refused with "don't ask again").
     */
    data class LocationPermissionResult(val granted: Boolean, val canAskAgain: Boolean = true) : NetworkSettingsUiEvent

    /** "Permitir ubicación": ask for the location permission again. */
    data object RequestLocationPermission : NetworkSettingsUiEvent

    /** The screen is visible again (e.g. back from Android's settings): re-read the network state. */
    data object ScreenResumed : NetworkSettingsUiEvent

    /** Navigate back from the Network settings screen. */
    data object BackPressed : NetworkSettingsUiEvent
}
