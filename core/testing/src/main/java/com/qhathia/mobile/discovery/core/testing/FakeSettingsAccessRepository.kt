package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SettingsAccessRepository] for tests. Validates against [password] when [pinRequired], and
 * can be set to fail to exercise the error path.
 */
class FakeSettingsAccessRepository(
    private val password: String = "246810",
    pinRequired: Boolean = true,
    var failure: Throwable? = null,
) : SettingsAccessRepository {

    private val pinRequiredFlow = MutableStateFlow(pinRequired)

    /** Whether a PIN is set; changing it is seen by [observePinRequired] collectors. */
    var pinRequired: Boolean
        get() = pinRequiredFlow.value
        set(value) {
            pinRequiredFlow.value = value
        }

    var validationCount: Int = 0
        private set

    override suspend fun isPinRequired(): Boolean = pinRequired

    override fun observePinRequired(): Flow<Boolean> = pinRequiredFlow

    override suspend fun validatePassword(input: String): Result<Boolean> {
        validationCount++
        return failure?.let { Result.failure(it) } ?: Result.success(pinRequired && input == password)
    }
}
