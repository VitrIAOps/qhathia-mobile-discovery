package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.usecase.IsSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePinLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RegisterPinFailureUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ReportPinLockoutUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResetPinAttemptsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateSettingsPasswordUseCase
import com.qhathia.mobile.discovery.core.testing.FakeDeviceLogRepository
import com.qhathia.mobile.discovery.core.testing.FakeSettingsAccessRepository
import com.qhathia.mobile.discovery.core.testing.FakeSettingsPinAttemptsRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.core.testing.MutableClock
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState
import com.qhathia.mobile.discovery.feature.settings.ui.SettingsViewModel
import com.qhathia.mobile.discovery.feature.settings.ui.formatMinutesSeconds
import kotlinx.coroutines.flow.first
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
    private val deviceLogs = FakeDeviceLogRepository()

    private fun newViewModel(
        repository: FakeSettingsAccessRepository,
        pinAttempts: FakeSettingsPinAttemptsRepository = attempts,
    ) = SettingsViewModel(
        validateSettingsPassword = ValidateSettingsPasswordUseCase(repository),
        isSettingsPinRequired = IsSettingsPinRequiredUseCase(repository),
        observePinLock = ObservePinLockUseCase(pinAttempts, clock),
        registerPinFailure = RegisterPinFailureUseCase(pinAttempts, clock),
        resetPinAttempts = ResetPinAttemptsUseCase(pinAttempts),
        observePinRequired = ObserveSettingsPinRequiredUseCase(repository),
        reportPinLockout = ReportPinLockoutUseCase(deviceLogs),
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
    fun given_fifth_wrong_pin_then_locked_for_30_seconds_and_keypad_ignores_input() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeSettingsAccessRepository(password = sixDigitPin)
            val vm = viewModel(repository)

            repeat(5) { vm.enterWrongPin() }
            assertEquals(30L, vm.uiState.value.lockedSecondsLeft)
            assertFalse(vm.uiState.value.isInputEnabled)

            // Even the right PIN is not checked while locked.
            vm.onEvent(SettingsUiEvent.PasswordChanged(sixDigitPin))
            assertEquals(5, repository.validationCount)
            assertEquals("", vm.uiState.value.passwordInput)
        }

    @Test
    fun given_locked_then_time_left_refreshes_every_second() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(5) { vm.enterWrongPin() }

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(29L, vm.uiState.value.lockedSecondsLeft)
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
    fun given_30_seconds_passed_then_unlocked_with_five_attempts_and_next_lock_is_1_minute() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()
            repeat(5) { vm.enterWrongPin() }

            advanceTimeBy(30_000)
            runCurrent()

            assertFalse(vm.uiState.value.isLocked)
            repeat(4) { vm.enterWrongPin() }
            assertFalse(vm.uiState.value.isLocked)
            vm.enterWrongPin()
            assertEquals(60L, vm.uiState.value.lockedSecondsLeft)
            vm.onEvent(SettingsUiEvent.DismissDialog)
        }

    @Test
    fun given_each_new_lock_then_reported_once_with_its_duration() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(5) { vm.enterWrongPin() }
        vm.enterWrongPin() // ignored while locked
        runCurrent()
        assertEquals(listOf(5 to 30L), deviceLogs.lockouts)

        advanceTimeBy(30_000)
        runCurrent()
        repeat(5) { vm.enterWrongPin() }
        runCurrent()

        assertEquals(listOf(5 to 30L, 5 to 60L), deviceLogs.lockouts)
        vm.onEvent(SettingsUiEvent.DismissDialog)
    }

    @Test
    fun given_report_fails_then_screen_is_unchanged() = runTest(mainDispatcher.dispatcher) {
        deviceLogs.result = Result.failure(IllegalStateException("offline"))
        val vm = viewModel()

        repeat(5) { vm.enterWrongPin() }
        runCurrent()

        assertEquals(30L, vm.uiState.value.lockedSecondsLeft)
        assertFalse(vm.uiState.value.showError)
        assertEquals(1, deviceLogs.lockouts.size)
    }

    @Test
    fun given_reopened_while_locked_then_not_reported_again() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        repeat(5) { vm.enterWrongPin() }
        vm.onEvent(SettingsUiEvent.DismissDialog)

        viewModel()
        runCurrent()

        assertEquals(1, deviceLogs.lockouts.size)
    }

    @Test
    fun given_pin_removed_while_dialog_open_then_unlocks() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeSettingsAccessRepository(password = sixDigitPin)
        val vm = viewModel(repository)
        repeat(5) { vm.enterWrongPin() }

        vm.uiEffect.test {
            repository.pinRequired = false
            assertTrue(awaitItem() is SettingsUiEffect.Unlocked)
        }
    }

    @Test
    fun lock_time_is_formatted_as_minutes_and_seconds() {
        assertEquals("0:30", formatMinutesSeconds(30))
        assertEquals("0:01", formatMinutesSeconds(1))
        assertEquals("1:00", formatMinutesSeconds(60))
        assertEquals("15:00", formatMinutesSeconds(900))
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

    // --- fix-pin-lockout-log (F-13) ---

    @Test
    fun given_store_emits_before_resume_then_new_lock_still_reported() = runTest(mainDispatcher.dispatcher) {
        // Worst case with DataStore: the screen already shows the new lock before submitPassword resumes.
        lateinit var vm: SettingsViewModel
        attempts.afterSet = { saved ->
            if (saved.lockedAt != null) vm.uiState.first { it.isLocked }
        }
        vm = viewModel()

        repeat(5) { vm.enterWrongPin() }
        runCurrent()

        assertEquals(30L, vm.uiState.value.lockedSecondsLeft)
        assertEquals(listOf(5 to 30L), deviceLogs.lockouts)
        vm.onEvent(SettingsUiEvent.DismissDialog)
    }

    @Test
    fun given_dialog_closed_right_after_lock_then_still_reported() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        repeat(5) { vm.enterWrongPin() }
        vm.onEvent(SettingsUiEvent.DismissDialog)
        runCurrent()

        assertEquals(listOf(5 to 30L), deviceLogs.lockouts)
    }
}
