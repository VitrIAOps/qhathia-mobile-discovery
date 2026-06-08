package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import javax.inject.Inject

/**
 * Validates the settings-unlock password through the [SettingsAccessRepository] port. Returns the
 * repository's [Result]: `true` when the password unlocks settings, `false` when it is rejected.
 */
class ValidateSettingsPasswordUseCase @Inject constructor(
    private val repository: SettingsAccessRepository,
) {
    suspend operator fun invoke(input: String): Result<Boolean> = repository.validatePassword(input)
}
