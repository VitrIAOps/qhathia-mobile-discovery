package com.qhathia.mobile.discovery.feature.settings.model

/** One-shot intents emitted by the Network settings screen. */
sealed interface NetworkSettingsUiEffect {

    /** Leave the Network settings screen. */
    data object NavigateBack : NetworkSettingsUiEffect

    /** Open Android's mobile-data screen (kiosk mode was already left for the moment). */
    data object OpenMobileDataSettings : NetworkSettingsUiEffect

    /** Ask for the location permission (Wi-Fi scans need it). */
    data object RequestLocationPermission : NetworkSettingsUiEffect
}
