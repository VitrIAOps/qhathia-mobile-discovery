package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository

/**
 * In-memory [SettingsAccessRepository] for tests. Validates against [password] when [pinRequired], and
 * can be set to fail to exercise the error path.
 */
class FakeSettingsAccessRepository(
    private val password: String = "246810",
    var pinRequired: Boolean = true,
    var failure: Throwable? = null,
) : SettingsAccessRepository {

    var validationCount: Int = 0
        private set

    override suspend fun isPinRequired(): Boolean = pinRequired

    override suspend fun validatePassword(input: String): Result<Boolean> {
        validationCount++
        return failure?.let { Result.failure(it) } ?: Result.success(pinRequired && input == password)
    }
}
