package com.qhathia.mobile.discovery.core.domain

/**
 * Settings-access rules. The PIN is the tablet's remote exit PIN, set from the staff app; the backend
 * accepts 4 to 6 digits (api-contract.md §21).
 */
object SettingsConstants {

    /** Shortest valid PIN: the confirm key enables from this length. */
    const val MIN_PIN_LENGTH: Int = 4

    /** Longest valid PIN: reaching it validates automatically and further digits are ignored. */
    const val MAX_PIN_LENGTH: Int = 6
}
