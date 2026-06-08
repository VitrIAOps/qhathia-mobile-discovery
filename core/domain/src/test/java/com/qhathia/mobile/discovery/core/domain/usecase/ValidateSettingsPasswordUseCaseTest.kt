package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidateSettingsPasswordUseCaseTest {

    /** Mirrors the production rule (validate against the mocked password) without an Android dependency. */
    private val repository = object : SettingsAccessRepository {
        override suspend fun validatePassword(input: String): Result<Boolean> =
            Result.success(input == SettingsConstants.MOCK_SETTINGS_PASSWORD)
    }

    private val useCase = ValidateSettingsPasswordUseCase(repository)

    @Test
    fun given_correct_password_when_validated_then_returns_true() = runTest {
        val result = useCase("1234")

        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun given_wrong_password_when_validated_then_returns_false() = runTest {
        assertFalse(useCase("0000").getOrNull() == true)
        assertFalse(useCase("").getOrNull() == true)
        assertFalse(useCase("12345").getOrNull() == true)
    }

    @Test
    fun given_expected_length_is_four_then_mock_password_matches_it() {
        assertEquals(SettingsConstants.SETTINGS_PASSWORD_LENGTH, SettingsConstants.MOCK_SETTINGS_PASSWORD.length)
    }
}
