package com.qhathia.mobile.discovery.feature.settings.model

/** User intents on the kiosk-exit item of the settings side menu. */
sealed interface KioskExitUiEvent {
    /** "Salir del modo kiosko" tapped. */
    data object ExitKioskMode : KioskExitUiEvent
}
