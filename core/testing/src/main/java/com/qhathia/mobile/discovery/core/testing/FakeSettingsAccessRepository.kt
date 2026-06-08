package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository

/**
 * In-memory [SettingsAccessRepository] for tests. Validates against [password] (defaulting to the
 * mocked production value) and can be set to fail to exercise the error path.
 */
class FakeSettingsAccessRepository(
    private val password: String = SettingsConstants.MOCK_SETTINGS_PASSWORD,
    var failure: Throwable? = null,
) : SettingsAccessRepository {

    override suspend fun validatePassword(input: String): Result<Boolean> =
        failure?.let { Result.failure(it) } ?: Result.success(input == password)
}
