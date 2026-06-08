package com.qhathia.mobile.discovery.core.data.settings

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsAccessRepositoryImplTest {

    private val repository = SettingsAccessRepositoryImpl(MockSettingsPasswordDataSource())

    @Test
    fun given_correct_password_when_validated_then_success_true() = runTest {
        val result = repository.validatePassword("1234")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun given_wrong_password_when_validated_then_success_false() = runTest {
        val result = repository.validatePassword("4321")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }
}
