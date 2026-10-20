package com.qhathia.mobile.discovery.core.domain.repository

/**
 * Port for the settings-unlock PIN, which is the tablet's remote exit PIN ("PIN de salida"). Validation
 * is local and offline against the stored hash.
 */
interface SettingsAccessRepository {

    /** Whether an exit PIN is set. When `false` the settings open without asking for a PIN. */
    suspend fun isPinRequired(): Boolean

    /**
     * Validates [input] against the exit PIN.
     *
     * @return [Result] wrapping `true` when the PIN is correct and `false` when it is wrong (or no PIN
     *   is set); a [Result.failure] only when validation could not be performed.
     */
    suspend fun validatePassword(input: String): Result<Boolean>
}
