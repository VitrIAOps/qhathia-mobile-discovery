package com.qhathia.mobile.discovery.core.domain.model

import java.time.Instant

/**
 * Stored wrong-PIN attempts of the settings gate.
 *
 * @property failedCount consecutive wrong PINs since the last success or unlock.
 * @property lockedAt when the gate locked (wall clock), or `null` while not locked.
 */
data class PinAttempts(val failedCount: Int = 0, val lockedAt: Instant? = null) {
    companion object {
        val NONE: PinAttempts = PinAttempts()
    }
}
