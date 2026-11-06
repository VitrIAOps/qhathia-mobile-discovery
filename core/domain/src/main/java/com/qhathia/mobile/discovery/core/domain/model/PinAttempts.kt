package com.qhathia.mobile.discovery.core.domain.model

import java.time.Instant

/**
 * Stored wrong-PIN attempts of the settings gate.
 *
 * @property failedCount consecutive wrong PINs since the last success or unlock.
 * @property lockedAt when the gate locked (wall clock), or `null` while not locked.
 * @property lockCount locks since the last correct PIN or PIN change; each one lasts longer
 *   (settings-pin-lockout D1). Kept when a lock ends.
 */
data class PinAttempts(
    val failedCount: Int = 0,
    val lockedAt: Instant? = null,
    val lockCount: Int = 0,
) {
    companion object {
        val NONE: PinAttempts = PinAttempts()
    }
}
