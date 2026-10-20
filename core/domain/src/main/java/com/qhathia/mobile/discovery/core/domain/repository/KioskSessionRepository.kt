package com.qhathia.mobile.discovery.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Port for the merchant's temporary exit from kiosk mode ("Salir del modo kiosko" in Configuración).
 * Process-scoped on purpose: it is never persisted, so every app start begins locked.
 */
interface KioskSessionRepository {

    /** `true` while the merchant has exited kiosk mode from Configuración. Starts `false`. */
    val exitedFromSettings: StateFlow<Boolean>

    fun setExitedFromSettings(exited: Boolean)
}
