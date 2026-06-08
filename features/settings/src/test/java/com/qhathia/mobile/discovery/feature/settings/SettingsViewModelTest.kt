package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
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

    private fun viewModel(repository: FakeSettingsAccessRepository = FakeSettingsAccessRepository()) =
        SettingsViewModel(ValidateSettingsPasswordUseCase(repository))

    @Test
    fun given_correct_pin_when_completed_then_emits_unlocked() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(SettingsUiEvent.PasswordChanged("1234"))
            assertTrue(awaitItem() is SettingsUiEffect.Unlocked)
        }
    }

    @Test
    fun given_wrong_pin_when_completed_then_clears_entry_with_error_and_no_unlock() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.uiEffect.test {
                vm.onEvent(SettingsUiEvent.PasswordChanged("0000"))
                expectNoEvents()
            }

            val state = vm.uiState.value
            assertTrue(state.showError)
            assertEquals("", state.passwordInput)
        }

    @Test
    fun given_input_changes_when_typed_then_clears_previous_error() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()
            vm.onEvent(SettingsUiEvent.PasswordChanged("0000"))
            assertTrue(vm.uiState.value.showError)

            vm.onEvent(SettingsUiEvent.PasswordChanged("1"))

            assertFalse(vm.uiState.value.showError)
        }

    @Test
    fun given_partial_pin_then_submit_disabled_until_expected_length() {
        // Completing the PIN auto-submits, so assert the gating property directly.
        assertFalse(SettingsUiState(passwordInput = "12").isSubmitEnabled)
        assertTrue(SettingsUiState(passwordInput = "1234").isSubmitEnabled)
    }

    @Test
    fun given_non_digits_when_typed_then_ignored() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(SettingsUiEvent.PasswordChanged("12ab"))

        assertEquals("12", vm.uiState.value.passwordInput)
    }

    @Test
    fun given_reset_when_dispatched_then_state_is_blank() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.onEvent(SettingsUiEvent.PasswordChanged("0000")) // wrong → sets error
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
