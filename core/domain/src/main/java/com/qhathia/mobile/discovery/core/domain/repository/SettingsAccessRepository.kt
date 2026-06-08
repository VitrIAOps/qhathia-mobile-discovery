package com.qhathia.mobile.discovery.core.domain.repository

/**
 * Port for validating the settings-unlock password. Implemented in `core:data` against a mocked
 * value for now; a real backend password API can replace the implementation without touching the
 * domain or presentation layers.
 */
interface SettingsAccessRepository {

    /**
     * Validates [input] against the settings password.
     *
     * @return [Result] wrapping `true` when the password is correct and `false` when it is wrong;
     *   a [Result.failure] only when validation could not be performed (e.g. a future backend error).
     */
    suspend fun validatePassword(input: String): Result<Boolean>
}
