package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.usecase.IsSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateSettingsPasswordUseCase
import com.qhathia.mobile.discovery.core.testing.FakeSettingsAccessRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState
import com.qhathia.mobile.discovery.feature.settings.ui.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val sixDigitPin = "246810"
    private val fourDigitPin = "2468"

    private fun viewModel(repository: FakeSettingsAccessRepository = FakeSettingsAccessRepository(password = sixDigitPin)) =
        SettingsViewModel(ValidateSettingsPasswordUseCase(repository), IsSettingsPinRequiredUseCase(repository))
            .also { it.onEvent(SettingsUiEvent.Reset) }

    @Test
    fun given_no_pin_set_when_opened_then_unlocks_without_dialog() = runTest(mainDispatcher.dispatcher) {
        val vm = SettingsViewModel(
            ValidateSettingsPasswordUseCase(FakeSettingsAccessRepository(pinRequired = false)),
            IsSettingsPinRequiredUseCase(FakeSettingsAccessRepository(pinRequired = false)),
        )

        vm.uiEffect.test {
            vm.onEvent(SettingsUiEvent.Reset)
            assertTrue(awaitItem() is SettingsUiEffect.Unlocked)
        }
        assertTrue(vm.uiState.value.isResolvingAccess)
    }

    @Test
    fun given_pin_set_when_opened_then_shows_keypad() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        assertFalse(vm.uiState.value.isResolvingAccess)
    }

    @Test
    fun given_correct_six_digit_pin_then_validates_automatically_and_unlocks() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(SettingsUiEvent.PasswordChanged(sixDigitPin))
            assertTrue(awaitItem() is SettingsUiEffect.Unlocked)
        }
    }

    @Test
    fun given_four_digits_then_not_submitted_until_confirm() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeSettingsAccessRepository(password = fourDigitPin)
        val vm = viewModel(repository)

        vm.uiEffect.test {
            vm.onEvent(SettingsUiEvent.PasswordChanged(fourDigitPin))
            expectNoEvents()
            assertEquals(0, repository.validationCount)

            vm.onEvent(SettingsUiEvent.SubmitPassword)
            assertTrue(awaitItem() is SettingsUiEffect.Unlocked)
        }
    }

    @Test
    fun given_fewer_than_four_digits_then_confirm_does_nothing() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeSettingsAccessRepository()
        val vm = viewModel(repository)

        vm.onEvent(SettingsUiEvent.PasswordChanged("123"))
        vm.onEvent(SettingsUiEvent.SubmitPassword)

        assertEquals(0, repository.validationCount)
        assertFalse(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun given_seventh_digit_then_ignored() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeSettingsAccessRepository(password = "999999"))

        vm.onEvent(SettingsUiEvent.PasswordChanged("1234567"))

        // Six digits were validated (wrong) and cleared; the seventh never counted.
        assertEquals("", vm.uiState.value.passwordInput)
        assertTrue(vm.uiState.value.showError)
    }

    @Test
    fun given_wrong_pin_then_clears_entry_with_error_and_no_unlock() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(SettingsUiEvent.PasswordChanged("1234"))
            vm.onEvent(SettingsUiEvent.SubmitPassword)
            expectNoEvents()
        }

        val state = vm.uiState.value
        assertTrue(state.showError)
        assertEquals("", state.passwordInput)
        assertFalse(state.isValidating)
    }

    @Test
    fun given_validation_fails_then_treated_as_wrong_pin() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeSettingsAccessRepository(failure = IllegalStateException("boom"))
        val vm = viewModel(repository)

        vm.onEvent(SettingsUiEvent.PasswordChanged(sixDigitPin))

        assertTrue(vm.uiState.value.showError)
    }

    @Test
    fun given_input_changes_when_typed_then_clears_previous_error() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.onEvent(SettingsUiEvent.PasswordChanged("000000"))
        assertTrue(vm.uiState.value.showError)

        vm.onEvent(SettingsUiEvent.PasswordChanged("1"))

        assertFalse(vm.uiState.value.showError)
    }

    @Test
    fun dots_and_confirm_follow_entry_length() {
        assertEquals(4, SettingsUiState(passwordInput = "12").pinDotCount)
        assertEquals(5, SettingsUiState(passwordInput = "12345").pinDotCount)
        assertFalse(SettingsUiState(passwordInput = "123").isSubmitEnabled)
        assertTrue(SettingsUiState(passwordInput = "1234").isSubmitEnabled)
        assertFalse(SettingsUiState(passwordInput = "1234", isValidating = true).isSubmitEnabled)
    }

    @Test
    fun given_non_digits_when_typed_then_ignored() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(SettingsUiEvent.PasswordChanged("12ab"))

        assertEquals("12", vm.uiState.value.passwordInput)
    }

    @Test
    fun given_reset_when_dispatched_then_entry_is_blank() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.onEvent(SettingsUiEvent.PasswordChanged("000000"))
        assertTrue(vm.uiState.value.showError)

        vm.onEvent(SettingsUiEvent.Reset)

        val state = vm.uiState.value
        assertEquals("", state.passwordInput)
        assertFalse(state.showError)
    }

    @Test
    fun given_dialog_dismissed_then_emits_navigate_back() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(SettingsUiEvent.DismissDialog)
            assertTrue(awaitItem() is SettingsUiEffect.NavigateBack)
        }
    }
}
