package com.qhathia.mobile.discovery.core.data.settings

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import javax.inject.Inject

/**
 * [SettingsAccessRepository] over the stored exit PIN hash. Works offline: the hash is cached by the
 * remote-settings sync. No hash means no PIN is set, so the settings open without asking.
 */
class SettingsAccessRepositoryImpl @Inject constructor(
    private val localDataSource: DeviceSettingsLocalDataSource,
    private val verifier: ExitPinVerifier,
) : SettingsAccessRepository {

    override suspend fun isPinRequired(): Boolean = !localDataSource.exitPinHash().isNullOrBlank()

    override suspend fun validatePassword(input: String): Result<Boolean> = runCatching {
        val hash = localDataSource.exitPinHash()
        !hash.isNullOrBlank() && verifier.matches(input, hash)
    }
}
