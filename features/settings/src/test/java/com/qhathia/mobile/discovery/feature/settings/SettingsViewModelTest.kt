package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.usecase.IsSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePinLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RegisterPinFailureUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResetPinAttemptsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateSettingsPasswordUseCase
import com.qhathia.mobile.discovery.core.testing.FakeSettingsAccessRepository
import com.qhathia.mobile.discovery.core.testing.FakeSettingsPinAttemptsRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.core.testing.MutableClock
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState
import com.qhathia.mobile.discovery.feature.settings.ui.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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

    private val attempts = FakeSettingsPinAttemptsRepository()
    private val clock = MutableClock { mainDispatcher.dispatcher.scheduler.currentTime }

    private fun newViewModel(repository: FakeSettingsAccessRepository) = SettingsViewModel(
        validateSettingsPassword = ValidateSettingsPasswordUseCase(repository),
        isSettingsPinRequired = IsSettingsPinRequiredUseCase(repository),
        observePinLock = ObservePinLockUseCase(attempts, clock),
        registerPinFailure = RegisterPinFailureUseCase(attempts, clock),
        resetPinAttempts = ResetPinAttemptsUseCase(attempts),
    )

    private fun viewModel(repository: FakeSettingsAccessRepository = FakeSettingsAccessRepository(password = sixDigitPin)) =
        newViewModel(repository).also { it.onEvent(SettingsUiEvent.Reset) }

    private fun SettingsViewModel.enterWrongPin() = onEvent(SettingsUiEvent.PasswordChanged("000000"))

    @Test
    fun given_no_pin_set_when_opened_then_unlocks_without_dialog() = runTest(mainDispatcher.dispatcher) {
        val vm = newViewModel(FakeSettingsAccessRepository(pinRequired = false))

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
    fun given_validation_fails_then_shows_error_without_counting_an_attempt() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeSettingsAccessRepository(failure = IllegalStateException("boom"))
        val vm = viewModel(repository)

        repeat(6) { vm.onEvent(SettingsUiEvent.PasswordChanged(sixDigitPin)) }

        assertTrue(vm.uiState.value.showError)
        assertFalse(vm.uiState.value.isLocked)
        assertEquals(PinAttempts.NONE, attempts.stored.value)
    }

    // --- Lockout (AJU-011) ---

    @Test
    fun given_four_wrong_pins_then_still_open() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        repeat(4) { vm.enterWrongPin() }

        assertFalse(vm.uiState.value.isLocked)
        assertTrue(vm.uiState.value.showError)
    }

    @Test
    fun given_fifth_wrong_pin_then_locked_for_24_hours_and_keypad_ignores_input() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeSettingsAccessRepository(password = sixDigitPin)
            val vm = viewModel(repository)

            repeat(5) { vm.enterWrongPin() }
            assertEquals(24 * 60L, vm.uiState.value.lockedMinutesLeft)
            assertFalse(vm.uiState.value.isInputEnabled)

            // Even the right PIN is not checked while locked.
            vm.onEvent(SettingsUiEvent.PasswordChanged(sixDigitPin))
            assertEquals(5, repository.validationCount)
            assertEquals("", vm.uiState.value.passwordInput)
        }

    @Test
    fun given_locked_then_time_left_refreshes_every_minute() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(5) { vm.enterWrongPin() }

        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(24 * 60L - 1, vm.uiState.value.lockedMinutesLeft)
    }

    @Test
    fun given_locked_when_gate_reopened_then_still_locked() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(5) { vm.enterWrongPin() }

        vm.onEvent(SettingsUiEvent.DismissDialog)
        val reopened = viewModel()

        assertTrue(reopened.uiState.value.isLocked)
    }

    @Test
    fun given_24_hours_passed_then_unlocked_with_five_attempts() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(5) { vm.enterWrongPin() }

        advanceTimeBy(24 * 60 * 60_000L)
        runCurrent()

        assertFalse(vm.uiState.value.isLocked)
        repeat(4) { vm.enterWrongPin() }
        assertFalse(vm.uiState.value.isLocked)
        vm.enterWrongPin()
        assertTrue(vm.uiState.value.isLocked)
        vm.onEvent(SettingsUiEvent.DismissDialog)
    }

    @Test
    fun given_correct_pin_after_three_wrong_then_count_resets() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(3) { vm.enterWrongPin() }

        vm.onEvent(SettingsUiEvent.PasswordChanged(sixDigitPin))

        assertEquals(PinAttempts.NONE, attempts.stored.value)
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
