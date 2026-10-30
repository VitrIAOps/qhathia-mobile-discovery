package com.qhathia.mobile.discovery.feature.productsearch

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.model.SearchFilterOption
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ComposeSearchQueryUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SearchProductsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunVoiceSearchSessionUseCase
import com.qhathia.mobile.discovery.core.testing.FakeAudioRecorder
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeProductSearchRepository
import com.qhathia.mobile.discovery.core.testing.FakeLiveTranscriptionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.core.testing.ProductFixtures
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEffect
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent
import com.qhathia.mobile.discovery.feature.productsearch.navigation.ARG_QUERY
import com.qhathia.mobile.discovery.feature.productsearch.navigation.ARG_SOURCE
import com.qhathia.mobile.discovery.feature.productsearch.ui.ProductSearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSearchViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        query: String = "taladro 20v",
        source: SearchSource = SearchSource.Text,
        repository: FakeProductSearchRepository = FakeProductSearchRepository(),
        recorder: FakeAudioRecorder = FakeAudioRecorder(),
        stt: FakeLiveTranscriptionRepository = FakeLiveTranscriptionRepository(),
        cartRepository: FakeCartRepository = FakeCartRepository(),
    ) = ProductSearchViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ARG_QUERY to query, ARG_SOURCE to source.name)),
        searchProducts = SearchProductsUseCase(repository),
        composeSearchQuery = ComposeSearchQueryUseCase(),
        runVoiceSearchSession = RunVoiceSearchSessionUseCase(recorder, stt),
        observeCart = ObserveCartUseCase(cartRepository),
        addToCart = AddToCartUseCase(cartRepository),
    )

    @Test
    fun given_results_when_initialized_then_results_phase() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(8))))
        val vm = viewModel(repository = repository)

        val phase = vm.uiState.value.phase
        assertTrue(phase is ProductSearchPhase.Results)
        assertEquals(8, (phase as ProductSearchPhase.Results).products.size)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun given_voice_search_in_flight_then_mic_animation_shown() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(delayMillis = 25_000)
        val vm = viewModel(source = SearchSource.Voice, repository = repository)

        assertEquals(ProductSearchPhase.Searching, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.showProcessingMicAnimation)

        advanceTimeBy(26_000)
    }

    @Test
    fun given_text_search_in_flight_then_no_mic_animation() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(delayMillis = 25_000)
        val vm = viewModel(source = SearchSource.Text, repository = repository)

        assertFalse(vm.uiState.value.showProcessingMicAnimation)

        advanceTimeBy(26_000)
    }

    @Test
    fun given_slow_search_after_20s_then_slow_phase() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(delayMillis = 25_000)
        val vm = viewModel(repository = repository)

        advanceTimeBy(ProductSearchConstants.SLOW_SEARCH_SECONDS * 1_000 + 1)

        assertEquals(ProductSearchPhase.SlowSearch, vm.uiState.value.phase)
        advanceTimeBy(6_000)
    }

    @Test
    fun given_no_results_then_shows_unhappy_with_response_suggestions() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(
            result = Result.success(ProductFixtures.noResults(products = ProductFixtures.products(3))),
        )
        val vm = viewModel(query = "taladro 20v", repository = repository)

        val phase = vm.uiState.value.phase
        assertTrue(phase is ProductSearchPhase.Unhappy)
        assertEquals(ProductSearchConstants.ALTERNATIVES_COUNT, (phase as ProductSearchPhase.Unhappy).alternatives.size)
        // A single request — the unhappy suggestions come from the same response, no second search.
        assertEquals(1, repository.callCount)
    }

    @Test
    fun given_unhappy_response_then_carries_suggested_category() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(
            result = Result.success(ProductFixtures.noResults(suggestedFromCategory = "lámpara")),
        )
        val vm = viewModel(repository = repository)

        val phase = vm.uiState.value.phase as ProductSearchPhase.Unhappy
        assertEquals("lámpara", phase.suggestedFromCategory)
    }

    @Test
    fun given_search_api_error_then_shows_unhappy_without_suggestions() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(
            result = Result.failure(SearchException(SearchErrorCategory.SearchApi)),
        )
        val vm = viewModel(repository = repository)

        val phase = vm.uiState.value.phase
        assertTrue(phase is ProductSearchPhase.Unhappy)
        phase as ProductSearchPhase.Unhappy
        assertTrue(phase.alternatives.isEmpty())
        assertNull(phase.suggestedFromCategory)
        // A single request — no disguised alternative search.
        assertEquals(1, repository.callCount)
    }

    @Test
    fun given_network_error_then_keeps_screen_without_alternatives() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(
            result = Result.failure(SearchException(SearchErrorCategory.Network)),
        )
        val vm = viewModel(repository = repository)

        assertEquals(ProductSearchPhase.NetworkError, vm.uiState.value.phase)
        assertEquals(1, repository.callCount) // no alternative search for a network error
    }

    @Test
    fun given_unhappy_idle_30s_then_navigates_home() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(result = Result.success(ProductFixtures.noResults()))
        val vm = viewModel(repository = repository)

        vm.uiEffect.test {
            advanceTimeBy(ProductSearchConstants.UNHAPPY_IDLE_TIMEOUT_SECONDS * 1_000 + 1)
            assertEquals(ProductSearchUiEffect.NavigateHomeIdle, awaitItem())
        }
    }

    @Test
    fun given_history_when_back_then_returns_previous_result() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(
            result = Result.success(ProductFixtures.result(displayText = "first")),
            alternativeResult = Result.success(ProductFixtures.result(displayText = "second")),
        )
        val vm = viewModel(repository = repository)

        vm.onEvent(ProductSearchUiEvent.QueryChanged("martillo"))
        vm.onEvent(ProductSearchUiEvent.SubmitSearch)
        assertEquals("second", (vm.uiState.value.phase as ProductSearchPhase.Results).displayText)
        assertTrue(vm.uiState.value.canGoBack)

        vm.onEvent(ProductSearchUiEvent.Back)
        assertEquals("first", (vm.uiState.value.phase as ProductSearchPhase.Results).displayText)
        assertFalse(vm.uiState.value.canGoBack)
    }

    @Test
    fun given_no_previous_result_when_back_then_opens_dialog() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(ProductSearchUiEvent.Back)

        assertTrue(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_confirm_new_search_then_navigates_home_idle() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.NewSearch)
            vm.onEvent(ProductSearchUiEvent.ConfirmCloseSearch)
            assertEquals(ProductSearchUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_activate_mic_then_listens_in_place_without_navigating() = runTest(mainDispatcher.dispatcher) {
        val recorder = FakeAudioRecorder()
        val vm = viewModel(recorder = recorder, stt = FakeLiveTranscriptionRepository(partials = emptyList()))

        vm.onEvent(ProductSearchUiEvent.ActivateMic)

        assertEquals(ProductSearchPhase.VoiceListening(liveTranscript = ""), vm.uiState.value.phase)
        assertTrue(recorder.isRecording)
        assertTrue(vm.uiState.value.showProcessingMicAnimation)
        vm.onEvent(ProductSearchUiEvent.CancelVoice)
    }

    @Test
    fun given_listening_when_words_recognized_then_live_transcript_in_phase() = runTest(mainDispatcher.dispatcher) {
        val stt = FakeLiveTranscriptionRepository(partials = listOf("mar", "martillo de"))
        val vm = viewModel(stt = stt)

        vm.onEvent(ProductSearchUiEvent.ActivateMic)

        assertEquals(ProductSearchPhase.VoiceListening(liveTranscript = "martillo de"), vm.uiState.value.phase)
        vm.onEvent(ProductSearchUiEvent.CancelVoice)
    }

    @Test
    fun given_stt_failure_when_stopped_then_restores_previous_screen_without_search() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(4))))
            val stt = FakeLiveTranscriptionRepository(finalFailure = SttException(SttErrorCategory.Network))
            val vm = viewModel(repository = repository, stt = stt) // init settles on Results

            vm.onEvent(ProductSearchUiEvent.ActivateMic)
            vm.onEvent(ProductSearchUiEvent.StopRecording)

            assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
            assertEquals(1, repository.callCount)
        }

    @Test
    fun given_slow_final_transcript_then_voice_slow_response_phase() = runTest(mainDispatcher.dispatcher) {
        val stt = FakeLiveTranscriptionRepository(finalText = "martillo", finalDelayMillis = 40_000)
        val vm = viewModel(stt = stt)

        vm.onEvent(ProductSearchUiEvent.ActivateMic)
        vm.onEvent(ProductSearchUiEvent.StopRecording)
        assertTrue(vm.uiState.value.phase is ProductSearchPhase.VoiceTranscribing)

        advanceTimeBy(31_000)
        assertEquals(ProductSearchPhase.VoiceSlowResponse, vm.uiState.value.phase)
        vm.onEvent(ProductSearchUiEvent.CancelVoice)
    }

    @Test
    fun given_results_arrive_after_recording_ends_then_transcribing_shows_them() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(
                partials = emptyList(),
                latePartials = listOf("marti", "martillo"),
                finalText = "martillo",
                finalDelayMillis = 1_000,
            )
            val vm = viewModel(stt = stt)

            vm.onEvent(ProductSearchUiEvent.ActivateMic)
            vm.onEvent(ProductSearchUiEvent.StopRecording)

            assertEquals(ProductSearchPhase.VoiceTranscribing(liveTranscript = "martillo"), vm.uiState.value.phase)
            vm.onEvent(ProductSearchUiEvent.CancelVoice)
        }

    @Test
    fun given_voice_transcript_when_stop_then_runs_search_in_place() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(4))))
        val stt = FakeLiveTranscriptionRepository(finalText = "martillo")
        val vm = viewModel(repository = repository, stt = stt)

        vm.onEvent(ProductSearchUiEvent.ActivateMic)
        vm.onEvent(ProductSearchUiEvent.StopRecording)

        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
        // Original init search + the voice-driven search.
        assertEquals(2, repository.callCount)
        assertEquals("martillo", repository.queries.last())
    }

    @Test
    fun given_voice_capture_when_cancelled_then_restores_previous_screen() = runTest(mainDispatcher.dispatcher) {
        val recorder = FakeAudioRecorder()
        val vm = viewModel(recorder = recorder) // init settles on Results

        vm.onEvent(ProductSearchUiEvent.ActivateMic)
        assertTrue(vm.uiState.value.phase is ProductSearchPhase.VoiceListening)

        vm.onEvent(ProductSearchUiEvent.CancelVoice)

        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
        assertTrue(recorder.cancelled)
    }

    // --- Catalog microphone: Voice hand-off without a query (CAT-030) ---

    @Test
    fun given_voice_and_empty_query_then_listens_and_sends_no_search() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository()
        val recorder = FakeAudioRecorder()
        val vm = viewModel(
            query = "",
            source = SearchSource.Voice,
            repository = repository,
            recorder = recorder,
            stt = FakeLiveTranscriptionRepository(partials = emptyList()),
        )

        assertEquals(ProductSearchPhase.VoiceListening(liveTranscript = ""), vm.uiState.value.phase)
        assertTrue(recorder.isRecording)
        assertEquals(0, repository.callCount)
        vm.onEvent(ProductSearchUiEvent.CancelVoice)
    }

    @Test
    fun given_voice_and_empty_query_when_transcribed_then_searches_the_transcript() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(4))))
            val vm = viewModel(
                query = "",
                source = SearchSource.Voice,
                repository = repository,
                stt = FakeLiveTranscriptionRepository(finalText = "martillo"),
            )

            vm.onEvent(ProductSearchUiEvent.StopRecording)

            assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
            assertEquals(listOf("martillo"), repository.queries)
        }

    @Test
    fun given_voice_and_query_then_searches_as_before() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(4))))
        val recorder = FakeAudioRecorder()
        val vm = viewModel(query = "taladro", source = SearchSource.Voice, repository = repository, recorder = recorder)

        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
        assertEquals(listOf("taladro"), repository.queries)
        assertFalse(recorder.isRecording)
    }

    @Test
    fun given_voice_entry_when_cancelled_then_navigates_back() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository()
        val vm = viewModel(
            query = "",
            source = SearchSource.Voice,
            repository = repository,
            stt = FakeLiveTranscriptionRepository(partials = emptyList()),
        )

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.CancelVoice)
            assertEquals(ProductSearchUiEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, repository.callCount)
    }

    @Test
    fun given_voice_entry_when_stt_fails_then_navigates_back_without_search() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository()
        val vm = viewModel(
            query = "",
            source = SearchSource.Voice,
            repository = repository,
            stt = FakeLiveTranscriptionRepository(finalFailure = SttException(SttErrorCategory.Network)),
        )

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.StopRecording)
            assertEquals(ProductSearchUiEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, repository.callCount)
    }

    @Test
    fun given_cart_tapped_then_product_added_and_count_updates() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(8))))
        val cartRepository = FakeCartRepository()
        val vm = viewModel(repository = repository, cartRepository = cartRepository)

        vm.onEvent(ProductSearchUiEvent.CartTapped("p1"))

        assertEquals(listOf("p1"), cartRepository.current.items.map { it.productId })
        assertEquals(1, vm.uiState.value.cartItemCount)
    }

    @Test
    fun given_open_cart_then_emits_open_cart_effect() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(ProductFixtures.products(4))))
        val vm = viewModel(repository = repository)

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.OpenCart)
            assertEquals(ProductSearchUiEffect.OpenCart, awaitItem())
        }
    }

    @Test
    fun given_query_over_cap_when_changed_then_truncated() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(ProductSearchUiEvent.QueryChanged("a".repeat(ProductSearchConstants.MAX_QUERY_CHARS + 50)))

        assertEquals(ProductSearchConstants.MAX_QUERY_CHARS, vm.uiState.value.query.length)
    }

    @Test
    fun given_suggestion_selected_then_requeries_with_it_appended() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.SuggestionSelected("Para uso en casa"))

        assertEquals("taladro 20v para uso en casa", repository.queries.last())
        assertEquals("Para uso en casa", vm.uiState.value.selectedSuggestion)
    }

    @Test
    fun given_multiple_filters_selected_then_requeries_with_all_appended() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Voltaje", "12V"))

        assertEquals("taladro 20v marca bosch voltaje 12v", repository.queries.last())
        assertEquals(2, vm.uiState.value.selectedFilters.size)
    }

    @Test
    fun given_second_suggestion_selected_then_replaces_the_first() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.SuggestionSelected("Para uso en casa"))
        vm.onEvent(ProductSearchUiEvent.SuggestionSelected("Percutor"))

        assertEquals("taladro 20v percutor", repository.queries.last())
        assertEquals("Percutor", vm.uiState.value.selectedSuggestion)
    }

    @Test
    fun given_criterion_cleared_then_drops_only_that_criterions_options() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Makita"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Voltaje", "12V"))

        vm.onEvent(ProductSearchUiEvent.FilterCriterionCleared("Marca"))

        assertEquals("taladro 20v voltaje 12v", repository.queries.last())
        assertEquals(setOf(SearchFilterOption("Voltaje", "12V")), vm.uiState.value.selectedFilters)
    }

    @Test
    fun given_criterion_without_selection_when_cleared_then_no_requery() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)
        val queriesBefore = repository.queries.size

        vm.onEvent(ProductSearchUiEvent.FilterCriterionCleared("Marca"))

        assertEquals(queriesBefore, repository.queries.size)
    }

    @Test
    fun given_all_selection_removed_then_requeries_the_base() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch")) // de-select

        assertEquals("taladro 20v", repository.queries.last())
        assertTrue(vm.uiState.value.selectedFilters.isEmpty())
    }

    @Test
    fun given_empty_requery_result_then_follows_unhappy_path() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(
            result = Result.success(ProductFixtures.result()),
            alternativeResult = Result.success(ProductFixtures.noResults()),
        )
        val vm = viewModel(query = "taladro 20v", repository = repository)
        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)

        vm.onEvent(ProductSearchUiEvent.SuggestionSelected("Percutor"))

        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Unhappy)
    }

    @Test
    fun given_selection_when_new_search_confirmed_then_selection_reset() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        assertTrue(vm.uiState.value.selectedFilters.isNotEmpty())

        vm.onEvent(ProductSearchUiEvent.NewSearch)
        vm.onEvent(ProductSearchUiEvent.ConfirmCloseSearch)

        assertTrue(vm.uiState.value.selectedFilters.isEmpty())
        assertNull(vm.uiState.value.selectedSuggestion)
    }
}
