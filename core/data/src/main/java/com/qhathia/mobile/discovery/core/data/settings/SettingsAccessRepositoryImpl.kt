package com.qhathia.mobile.discovery.core.data.settings

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import javax.inject.Inject

/**
 * [SettingsAccessRepository] delegating to [MockSettingsPasswordDataSource]. Validation is local and
 * cannot fail today, so it always wraps the boolean outcome in [Result.success]; a future backend
 * implementation can surface real errors via [Result.failure] without changing callers.
 */
class SettingsAccessRepositoryImpl @Inject constructor(
    private val dataSource: MockSettingsPasswordDataSource,
) : SettingsAccessRepository {

    override suspend fun validatePassword(input: String): Result<Boolean> =
        Result.success(dataSource.validate(input))
}
