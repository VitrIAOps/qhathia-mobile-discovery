package com.qhathia.mobile.discovery.core.domain.model

/**
 * The pairing code on screen and when it expires, kept while the pairing section is closed so it is
 * shown again instead of generating a new one (design D6).
 *
 * @property code the generated code and its session.
 * @property expiresAtElapsedMillis expiry on the monotonic clock that counts deep sleep
 *   (`SystemClock.elapsedRealtime`), so a screen off/on is counted.
 */
data class HeldPairingCode(val code: PairingCode, val expiresAtElapsedMillis: Long)
