package com.qhathia.mobile.discovery.feature.home

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEffect
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEvent
import com.qhathia.mobile.discovery.feature.home.model.MicState
import com.qhathia.mobile.discovery.feature.home.ui.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        repository: FakeStoreSessionRepository = FakeStoreSessionRepository(),
    ) = HomeViewModel(ObserveStoreSessionUseCase(repository))

    @Test
    fun given_store_session_has_name_when_observed_then_store_name_surfaces() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeStoreSessionRepository(StoreSession(storeName = "Joyería María"))
            val vm = viewModel(repository)

            assertEquals("Joyería María", vm.uiState.value.storeName)
        }

    @Test
    fun given_idle_when_activate_mic_then_controls_are_hidden_and_listening() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.onEvent(HomeUiEvent.ActivateMic)

            assertTrue(vm.uiState.value.micState is MicState.Listening)
            assertFalse(vm.uiState.value.areControlsVisible)
        }

    @Test
    fun given_listening_when_timeout_elapses_then_listening_ends_with_retry_prompt() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.onEvent(HomeUiEvent.ActivateMic)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.micState is MicState.NoAudioRetry)
        }

    @Test
    fun given_consecutive_timeouts_then_retry_prompt_varies() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.onEvent(HomeUiEvent.ActivateMic)
            advanceUntilIdle()
            val first = (vm.uiState.value.micState as MicState.NoAudioRetry).promptIndex

            vm.onEvent(HomeUiEvent.RetryMic)
            advanceUntilIdle()
            val second = (vm.uiState.value.micState as MicState.NoAudioRetry).promptIndex

            assertNotEquals(first, second)
        }

    @Test
    fun given_listening_when_cancelled_then_returns_to_idle_with_controls() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.CancelMic)

            assertEquals(MicState.Idle, vm.uiState.value.micState)
            assertTrue(vm.uiState.value.areControlsVisible)
        }

    @Test
    fun given_open_catalog_when_event_then_navigation_effect_emitted() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.OpenCatalog)
                assertEquals(HomeUiEffect.NavigateToCatalog, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun given_query_when_submit_search_then_results_navigation_with_query() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.QueryChanged("lamps"))
                vm.onEvent(HomeUiEvent.SubmitSearch)
                assertEquals(HomeUiEffect.NavigateToResults("lamps"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
