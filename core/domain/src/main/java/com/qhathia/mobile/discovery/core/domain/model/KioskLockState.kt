package com.qhathia.mobile.discovery.core.domain.model

/**
 * Whether the tablet must be locked on Discovery right now (screen pinning, no system bars, screen on).
 *
 * @property active `true` when the device is paired, the remote `kioskMode` is on and the merchant has
 *   not exited kiosk mode from Configuración in the current app process.
 */
data class KioskLockState(val active: Boolean) {
    companion object {
        /** Not locked: used before the inputs are known, so an unpaired tablet is never pinned. */
        val INACTIVE: KioskLockState = KioskLockState(active = false)
    }
}
