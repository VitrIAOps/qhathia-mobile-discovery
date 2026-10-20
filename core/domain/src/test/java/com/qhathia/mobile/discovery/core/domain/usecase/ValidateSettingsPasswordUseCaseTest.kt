package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.testing.FakeSettingsAccessRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidateSettingsPasswordUseCaseTest {

    private val repository = FakeSettingsAccessRepository(password = "246810")
    private val useCase = ValidateSettingsPasswordUseCase(repository)

    @Test
    fun given_correct_pin_when_validated_then_returns_true() = runTest {
        assertTrue(useCase("246810").getOrNull() == true)
    }

    @Test
    fun given_wrong_pin_when_validated_then_returns_false() = runTest {
        assertFalse(useCase("1234").getOrNull() == true)
        assertFalse(useCase("").getOrNull() == true)
    }

    @Test
    fun pin_required_reflects_repository() = runTest {
        assertTrue(IsSettingsPinRequiredUseCase(repository)())

        repository.pinRequired = false

        assertFalse(IsSettingsPinRequiredUseCase(repository)())
    }
}
