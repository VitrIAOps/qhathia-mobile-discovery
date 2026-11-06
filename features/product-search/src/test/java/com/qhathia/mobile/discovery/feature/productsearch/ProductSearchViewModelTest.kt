package com.qhathia.mobile.discovery.feature.productsearch

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import com.qhathia.mobile.discovery.core.domain.model.SearchFilterOption
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ComposeSearchQueryUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunVoiceSearchSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SearchProductsUseCase
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeAudioRecorder
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCustomerSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeLiveTranscriptionRepository
import com.qhathia.mobile.discovery.core.testing.FakeProductSearchRepository
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
        sessionRepository: FakeCustomerSessionRepository = FakeCustomerSessionRepository(),
    ) = ProductSearchViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ARG_QUERY to query, ARG_SOURCE to source.name)),
        searchProducts = SearchProductsUseCase(repository),
        composeSearchQuery = ComposeSearchQueryUseCase(),
        runVoiceSearchSession = RunVoiceSearchSessionUseCase(recorder, stt),
        observeCart = ObserveCartUseCase(cartRepository),
        addToCart = AddToCartUseCase(cartRepository),
        endCustomerSession = EndCustomerSessionUseCase(cartRepository, sessionRepository),
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
    fun given_no_previous_result_when_back_then_asks_the_screen_to_exit() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.Back)
            assertEquals(ProductSearchUiEffect.ExitRequested, awaitItem())
        }
        // The dialog is the screen's choice (only from Home), not the view model's.
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_network_error_after_a_result_when_back_then_previous_result_shown() =
        runTest(mainDispatcher.dispatcher) {
            val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result(displayText = "first")))
            val vm = viewModel(repository = repository) // init settles on "first"
            repository.result = Result.failure(SearchException(SearchErrorCategory.Network))
            vm.onEvent(ProductSearchUiEvent.QueryChanged("foco"))
            vm.onEvent(ProductSearchUiEvent.SubmitSearch)
            assertEquals(ProductSearchPhase.NetworkError, vm.uiState.value.phase)

            vm.uiEffect.test {
                vm.onEvent(ProductSearchUiEvent.Back)
                expectNoEvents()
            }

            assertEquals("first", (vm.uiState.value.phase as ProductSearchPhase.Results).displayText)
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

    @Test
    fun given_voice_capture_when_back_then_capture_cancelled_and_results_kept() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder()
            val vm = viewModel(recorder = recorder) // init settles on Results

            vm.onEvent(ProductSearchUiEvent.ActivateMic)
            vm.onEvent(ProductSearchUiEvent.Back)

            assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
            assertTrue(recorder.cancelled)
            assertFalse(vm.uiState.value.showCloseConfirmDialog)
        }

    @Test
    fun given_listening_when_screen_stopped_then_recorder_released_and_results_restored() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder()
            val vm = viewModel(recorder = recorder) // init settles on Results

            vm.onEvent(ProductSearchUiEvent.ActivateMic)
            vm.onEvent(ProductSearchUiEvent.ScreenStopped)

            assertTrue(vm.uiState.value.phase is ProductSearchPhase.Results)
            assertFalse(recorder.isRecording)
        }

    @Test
    fun given_voice_entry_listening_when_screen_stopped_then_leaves_once_resumed() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder()
            val vm = viewModel(
                query = "",
                source = SearchSource.Voice,
                recorder = recorder,
                stt = FakeLiveTranscriptionRepository(partials = emptyList()),
            )

            vm.uiEffect.test {
                vm.onEvent(ProductSearchUiEvent.ScreenStopped)
                assertFalse(recorder.isRecording)
                expectNoEvents()

                vm.onEvent(ProductSearchUiEvent.ScreenResumed)
                assertEquals(ProductSearchUiEffect.NavigateBack, awaitItem())

                vm.onEvent(ProductSearchUiEvent.ScreenResumed)
                expectNoEvents()
            }
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
    fun given_multiple_filters_selected_then_requeries_with_structured_filters() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Voltaje", "12V"))

        assertEquals("taladro 20v", repository.queries.last())
        assertEquals(StructuredSearchFilters(brands = listOf("Bosch"), tags = listOf("12V")), repository.filters.last())
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
    fun given_chip_selected_then_new_search_when_back_then_chip_selected_again() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro", repository = repository)
        vm.onEvent(ProductSearchUiEvent.SuggestionSelected("Percutor"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.QueryChanged("foco"))
        vm.onEvent(ProductSearchUiEvent.SubmitSearch)
        assertEquals(null, vm.uiState.value.selectedSuggestion)
        val requestsBeforeBack = repository.queries.size

        vm.onEvent(ProductSearchUiEvent.Back)

        assertEquals("Percutor", vm.uiState.value.selectedSuggestion)
        assertEquals(1, vm.uiState.value.selectedFilters.size)
        assertEquals(requestsBeforeBack, repository.queries.size) // no search on Back
    }

    @Test
    fun given_criterion_cleared_then_drops_only_that_criterions_options() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.result()))
        val vm = viewModel(query = "taladro 20v", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Makita"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Voltaje", "12V"))

        vm.onEvent(ProductSearchUiEvent.FilterCriterionCleared("Marca"))

        // The text stays the original query; the remaining chip travels as a structured filter (F-37).
        assertEquals("taladro 20v", repository.queries.last())
        assertEquals(StructuredSearchFilters(tags = listOf("12V")), repository.filters.last())
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

    @Test
    fun given_items_in_cart_when_close_confirmed_then_session_ends_before_going_home() =
        runTest(mainDispatcher.dispatcher) {
            val cartRepository = FakeCartRepository(CartFixtures.cart(listOf(CartFixtures.cartItem(quantity = 2))))
            val sessionRepository = FakeCustomerSessionRepository()
            val vm = viewModel(cartRepository = cartRepository, sessionRepository = sessionRepository)

            vm.uiEffect.test {
                vm.onEvent(ProductSearchUiEvent.NewSearch)
                vm.onEvent(ProductSearchUiEvent.ConfirmCloseSearch)
                assertEquals(ProductSearchUiEffect.NavigateHomeIdle, awaitItem())
                // Already cleared when the navigation effect arrives (CAR-040).
                assertTrue(cartRepository.current.items.isEmpty())
                assertEquals(1, sessionRepository.endCount)
            }
        }

    @Test
    fun given_no_history_when_back_confirmed_then_session_ends() = runTest(mainDispatcher.dispatcher) {
        val cartRepository = FakeCartRepository(CartFixtures.cart(listOf(CartFixtures.cartItem())))
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(cartRepository = cartRepository, sessionRepository = sessionRepository)

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.Back)
            assertEquals(ProductSearchUiEffect.ExitRequested, awaitItem())
            vm.onEvent(ProductSearchUiEvent.NewSearch) // what the screen does when Home is below
            assertTrue(vm.uiState.value.showCloseConfirmDialog)
            vm.onEvent(ProductSearchUiEvent.ConfirmCloseSearch)
            assertEquals(ProductSearchUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertTrue(cartRepository.current.items.isEmpty())
        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_close_dialog_when_dismissed_then_session_kept() = runTest(mainDispatcher.dispatcher) {
        val cartRepository = FakeCartRepository(CartFixtures.cart(listOf(CartFixtures.cartItem(quantity = 2))))
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(cartRepository = cartRepository, sessionRepository = sessionRepository)

        vm.onEvent(ProductSearchUiEvent.NewSearch)
        vm.onEvent(ProductSearchUiEvent.DismissDialog)

        assertFalse(vm.uiState.value.showCloseConfirmDialog)
        assertEquals(2, vm.uiState.value.cartItemCount)
        assertEquals(0, sessionRepository.endCount)
    }

    @Test
    fun given_items_in_cart_when_go_to_catalog_then_navigates_and_keeps_session() =
        runTest(mainDispatcher.dispatcher) {
            val cartRepository = FakeCartRepository(CartFixtures.cart(listOf(CartFixtures.cartItem(quantity = 2))))
            val sessionRepository = FakeCustomerSessionRepository()
            val vm = viewModel(cartRepository = cartRepository, sessionRepository = sessionRepository)

            vm.uiEffect.test {
                vm.onEvent(ProductSearchUiEvent.GoToCatalog)
                assertEquals(ProductSearchUiEffect.NavigateToCatalog, awaitItem())
            }
            assertFalse(vm.uiState.value.showCloseConfirmDialog)
            assertEquals(2, cartRepository.current.totalItemCount)
            assertEquals(0, sessionRepository.endCount)
        }

    @Test
    fun given_no_results_when_go_to_catalog_then_navigates() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductSearchRepository(Result.success(ProductFixtures.noResults()))
        val vm = viewModel(repository = repository)
        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Unhappy)

        vm.uiEffect.test {
            vm.onEvent(ProductSearchUiEvent.GoToCatalog)
            assertEquals(ProductSearchUiEffect.NavigateToCatalog, awaitItem())
            // The unhappy idle timeout was cancelled: no late return to Home.
            advanceTimeBy(ProductSearchConstants.UNHAPPY_IDLE_TIMEOUT_SECONDS * 1_000 + 1)
            expectNoEvents()
        }
    }

    // --- search-structured-filters (F-37) ---

    private val tagPanel = listOf(
        SearchFilter("Marca", listOf("Bosch", "DeWalt", "Makita")),
        SearchFilter("Tipo", listOf("Inalámbrico", "Con cable"), values = listOf("inalambrico", "con-cable")),
    )

    private fun searchWith(vararg results: ProductSearchResult) = FakeProductSearchRepository(Result.success(results.first())).apply {
        if (results.size > 1) alternativeResult = Result.success(results[1])
    }

    @Test
    fun filterToggled_sendsOriginalTextAndBrands() = runTest(mainDispatcher.dispatcher) {
        val repository = searchWith(ProductFixtures.result(filters = tagPanel))
        val vm = viewModel(query = "taladro", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))

        assertEquals(listOf("taladro", "taladro"), repository.queries)
        assertEquals(listOf(null, StructuredSearchFilters(brands = listOf("Bosch"))), repository.filters)
    }

    @Test
    fun tagChip_sendsValueNotLabel() = runTest(mainDispatcher.dispatcher) {
        val repository = searchWith(ProductFixtures.result(filters = tagPanel))
        val vm = viewModel(query = "taladro", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Tipo", "Inalámbrico"))

        assertEquals(StructuredSearchFilters(tags = listOf("inalambrico")), repository.filters.last())
    }

    @Test
    fun suggestion_staysInTextWithFilters() = runTest(mainDispatcher.dispatcher) {
        val repository = searchWith(ProductFixtures.result(filters = tagPanel, suggestions = listOf("Para uso doméstico")))
        val vm = viewModel(query = "taladro", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.SuggestionSelected("Para uso doméstico"))

        assertEquals("taladro para uso doméstico", repository.queries.last())
        assertEquals(StructuredSearchFilters(brands = listOf("Bosch")), repository.filters.last())
    }

    @Test
    fun clearingAllChips_sendsBaseQueryWithoutFilters() = runTest(mainDispatcher.dispatcher) {
        val repository = searchWith(ProductFixtures.result(filters = tagPanel))
        val vm = viewModel(query = "taladro", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))

        assertEquals("taladro", repository.queries.last())
        assertEquals(null, repository.filters.last())
    }

    @Test
    fun refinement_keepsBasePanel() = runTest(mainDispatcher.dispatcher) {
        val refined = ProductFixtures.result(
            products = ProductFixtures.products(1),
            filters = listOf(SearchFilter("Marca", listOf("Bosch"))),
            suggestions = emptyList(),
        )
        val repository = searchWith(ProductFixtures.result(filters = tagPanel), refined)
        val vm = viewModel(query = "taladro", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))

        val phase = vm.uiState.value.phase as ProductSearchPhase.Results
        assertEquals(tagPanel, phase.filters)
        assertEquals(ProductFixtures.suggestions(), phase.suggestions)
        assertEquals(1, phase.products.size)
    }

    @Test
    fun twoChips_noResults_keepsChipsAndPanel() = runTest(mainDispatcher.dispatcher) {
        val noMatch = ProductFixtures.result(
            products = ProductFixtures.products(3),
            noResults = true,
            fallbackMessage = "No hay productos con esos filtros",
            filters = emptyList(),
        )
        val repository = searchWith(ProductFixtures.result(filters = tagPanel), noMatch)
        val vm = viewModel(query = "taladro", repository = repository)

        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Tipo", "Inalámbrico"))

        val state = vm.uiState.value
        val phase = state.phase as ProductSearchPhase.Results
        assertTrue(phase.noFilterMatch)
        assertTrue(phase.products.isEmpty()) // the 3 unfiltered results are not shown as matches
        assertEquals(tagPanel, phase.filters)
        assertEquals(
            setOf(SearchFilterOption("Marca", "Bosch"), SearchFilterOption("Tipo", "Inalámbrico")),
            state.selectedFilters,
        )
    }

    @Test
    fun freshSearch_noResults_stillUnhappy() = runTest(mainDispatcher.dispatcher) {
        val repository = searchWith(ProductFixtures.result(products = ProductFixtures.products(2), noResults = true, filters = emptyList()))
        val vm = viewModel(query = "ventilador", repository = repository)

        assertTrue(vm.uiState.value.phase is ProductSearchPhase.Unhappy)
    }

    @Test
    fun alternativesOnly_setsMissingCategory() = runTest(mainDispatcher.dispatcher) {
        val alternatives = ProductFixtures.result(products = ProductFixtures.products(3))
            .copy(alternativesOnly = true, missingCategory = "ventilador")
        val vm = viewModel(query = "ventilador", repository = searchWith(alternatives))

        assertEquals("ventilador", (vm.uiState.value.phase as ProductSearchPhase.Results).missingCategory)
    }

    @Test
    fun back_restoresPanelAndChips() = runTest(mainDispatcher.dispatcher) {
        val refined = ProductFixtures.result(products = ProductFixtures.products(1), filters = emptyList())
        val repository = searchWith(ProductFixtures.result(filters = tagPanel), refined)
        val vm = viewModel(query = "taladro", repository = repository)
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Marca", "Bosch"))

        vm.onEvent(ProductSearchUiEvent.Back)
        vm.onEvent(ProductSearchUiEvent.FilterToggled("Tipo", "Inalámbrico"))

        // After Back the first view's panel is the base again, so the tag is sent by its value.
        assertEquals(StructuredSearchFilters(tags = listOf("inalambrico")), repository.filters.last())
        assertEquals(tagPanel, (vm.uiState.value.phase as ProductSearchPhase.Results).filters)
    }
}
