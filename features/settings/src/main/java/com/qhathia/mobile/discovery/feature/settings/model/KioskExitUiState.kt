package com.qhathia.mobile.discovery.feature.settings.model

/**
 * Kiosk-exit part of the settings side menu.
 *
 * @property isLockActive the kiosk lock is on: "Salir del modo kiosko" is offered.
 * @property hasExited the merchant exited from this settings visit: the temporary-exit message shows.
 */
data class KioskExitUiState(
    val isLockActive: Boolean = false,
    val hasExited: Boolean = false,
)
