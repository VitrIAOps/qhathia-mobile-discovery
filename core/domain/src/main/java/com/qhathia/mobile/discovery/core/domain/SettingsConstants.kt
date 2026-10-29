package com.qhathia.mobile.discovery.core.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Settings-access rules. The PIN is the tablet's remote exit PIN, set from the staff app; the backend
 * accepts 4 to 6 digits (api-contract.md §21).
 */
object SettingsConstants {

    /** Shortest valid PIN: the confirm key enables from this length. */
    const val MIN_PIN_LENGTH: Int = 4

    /** Longest valid PIN: reaching it validates automatically and further digits are ignored. */
    const val MAX_PIN_LENGTH: Int = 6

    /** Consecutive wrong PINs that lock the gate (design D9, AJU-011). */
    const val PIN_MAX_ATTEMPTS: Int = 5

    /** How long the gate stays locked after the last allowed wrong PIN. */
    val PIN_LOCK_DURATION: Duration = 24.hours

    /** How often the time left on a lock is refreshed on screen. */
    val PIN_LOCK_REFRESH_INTERVAL: Duration = 1.minutes
}
