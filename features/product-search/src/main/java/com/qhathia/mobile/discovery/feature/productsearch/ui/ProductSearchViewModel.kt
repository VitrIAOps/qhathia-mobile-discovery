package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants
import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import com.qhathia.mobile.discovery.core.domain.model.SearchFilterOption
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters
import com.qhathia.mobile.discovery.core.domain.model.VoiceSessionEvent
import com.qhathia.mobile.discovery.core.domain.model.toCartItem
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartResult
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ComposeSearchQueryUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunVoiceSearchSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SearchProductsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.VoiceSearchSession
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEffect
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiState
import com.qhathia.mobile.discovery.feature.productsearch.navigation.ARG_QUERY
import com.qhathia.mobile.discovery.feature.productsearch.navigation.ARG_SOURCE
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the product-search screen and owns its state machine:
 *
 * A single request per search: `Searching → (SlowSearch) → Results` when the response has
 * `noResults=false`, or `→ Unhappy` when `noResults=true` (rendering the up-to-3 store suggestions the
 * response already returned). A search (API) error settles `Unhappy` with no suggestions; a
 * connectivity failure settles `NetworkError` (screen kept). Each settled outcome is pushed onto an
 * in-memory history so Volver can return to the previous result; with no previous result Volver opens
 * the close-confirmation dialog. Confirming that dialog (from Nueva búsqueda or Volver) ends the
 * customer session (basket and customer data), clears the history and returns Home idle. The 30s idle
 * timeout on the unhappy screen returns Home without ending the session. Ir al catálogo opens the
 * catalog and keeps the session. The microphone animation is retained
 * while processing only for a voice-initiated search.
 */
@HiltViewModel
class ProductSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchProducts: SearchProductsUseCase,
    private val composeSearchQuery: ComposeSearchQueryUseCase,
    private val runVoiceSearchSession: RunVoiceSearchSessionUseCase,
    observeCart: ObserveCartUseCase,
    private val addToCart: AddToCartUseCase,
    private val endCustomerSession: EndCustomerSessionUseCase,
) : ViewModel() {

    private val initialQuery: String = savedStateHandle.get<String>(ARG_QUERY)?.trim().orEmpty()
    private val initialSource: SearchSource = runCatching {
        SearchSource.valueOf(savedStateHandle.get<String>(ARG_SOURCE) ?: SearchSource.Text.name)
    }.getOrDefault(SearchSource.Text)

    private val _uiState = MutableStateFlow(ProductSearchUiState(source = initialSource))
    val uiState: StateFlow<ProductSearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductSearchUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProductSearchUiEffect> = _uiEffect.asSharedFlow()

    /** Settled outcomes visited so far; the last entry is the one currently displayed. */
    private val history = mutableListOf<HistoryEntry>()

    /** Immutable raw query that started the current results view; the text base for refinements. */
    private var baseQuery: String = ""

    /** The chip panel of the fresh search that started the current results view; refinements keep it. */
    private var baseFilters: List<SearchFilter> = emptyList()

    /** The suggestions of the fresh search that started the current results view. */
    private var baseSuggestions: List<String> = emptyList()

    /** Filter options currently selected (insertion-ordered); N may be active across criteria. */
    private val selectedFilters = LinkedHashSet<SearchFilterOption>()

    /** The single selected suggestion phrase, or null when none is selected. */
    private var selectedSuggestion: String? = null

    private var searchJob: Job? = null
    private var slowSearchJob: Job? = null
    private var idleJob: Job? = null

    private var voiceSession: VoiceSearchSession? = null
    private var voiceJob: Job? = null
    private var voiceSlowResponseJob: Job? = null

    /** The phase/source to restore if an in-place voice capture is cancelled or yields no usable input. */
    private var phaseBeforeVoice: ProductSearchPhase? = null
    private var sourceBeforeVoice: SearchSource = SearchSource.Text

    /** Voice capture was stopped while hidden and there was nothing to restore: leave once shown again. */
    private var leaveOnResume = false

    init {
        // The Catalog microphone opens this screen with source Voice and no query: listen here
        // instead of sending an empty search (design D11, CAT-030).
        if (initialSource == SearchSource.Voice && initialQuery.isBlank()) {
            startVoiceCapture()
            phaseBeforeVoice = null // nothing to go back to on this screen
        } else {
            performSearch(initialQuery, initialSource)
        }
        viewModelScope.launch {
            observeCart().collect { cart ->
                _uiState.update { it.copy(cartItemCount = cart.totalItemCount) }
            }
        }
    }

    fun onEvent(event: ProductSearchUiEvent) {
        when (event) {
            ProductSearchUiEvent.Back -> onBack()
            ProductSearchUiEvent.NewSearch -> _uiState.update { it.copy(showCloseConfirmDialog = true) }
            ProductSearchUiEvent.GoToCatalog -> goToCatalog()
            ProductSearchUiEvent.ConfirmCloseSearch -> confirmCloseSearch()
            ProductSearchUiEvent.DismissDialog -> _uiState.update { it.copy(showCloseConfirmDialog = false) }
            is ProductSearchUiEvent.QueryChanged -> onQueryChanged(event.query)
            ProductSearchUiEvent.SubmitSearch -> onSubmitSearch()
            ProductSearchUiEvent.ActivateMic -> startVoiceCapture()
            ProductSearchUiEvent.StopRecording -> stopRecording()
            ProductSearchUiEvent.CancelVoice -> cancelVoiceCapture()
            is ProductSearchUiEvent.CartTapped -> onCartTapped(event.productId)
            ProductSearchUiEvent.OpenCart -> openCart()
            is ProductSearchUiEvent.ProductTapped -> openDetail(event.productId)
            is ProductSearchUiEvent.FilterToggled -> onFilterToggled(event.criterion, event.option)
            is ProductSearchUiEvent.FilterCriterionCleared -> onFilterCriterionCleared(event.criterion)
            is ProductSearchUiEvent.SuggestionSelected -> onSuggestionSelected(event.phrase)
            ProductSearchUiEvent.ScreenStopped -> onScreenStopped()
            ProductSearchUiEvent.ScreenResumed -> onScreenResumed()
        }
    }

    private fun onFilterToggled(criterion: String, option: String) {
        val ref = SearchFilterOption(criterion, option)
        if (!selectedFilters.add(ref)) selectedFilters.remove(ref)
        refineSearch()
    }

    private fun onFilterCriterionCleared(criterion: String) {
        if (selectedFilters.removeAll { it.criterion == criterion }) refineSearch()
    }

    private fun onSuggestionSelected(phrase: String) {
        selectedSuggestion = if (selectedSuggestion == phrase) null else phrase
        refineSearch()
    }

    /**
     * Re-query with the original [baseQuery] (plus the suggestion, which is natural language) and the
     * selected chips as structured filters (F-37): brands as labels, other criteria as their values.
     */
    private fun refineSearch() {
        val text = composeSearchQuery(baseQuery, selectedSuggestion)
        val filters = StructuredSearchFilters.from(selectedFilters, baseFilters).takeUnless { it.isEmpty }
        performSearch(text, _uiState.value.source, isRefinement = true, filters = filters)
    }

    private fun openDetail(productId: String) {
        resetIdleTimerIfUnhappy()
        emit(ProductSearchUiEffect.NavigateToDetail(productId))
    }

    private fun onCartTapped(productId: String) {
        resetIdleTimerIfUnhappy()
        val product = currentProducts().firstOrNull { it.id == productId } ?: return
        addAndReportLimit(product.toCartItem())
    }

    private fun openCart() {
        resetIdleTimerIfUnhappy()
        emit(ProductSearchUiEffect.OpenCart)
    }

    /** The products currently on screen (results or unhappy alternatives), for cart adds. */
    private fun currentProducts(): List<Product> = when (val phase = _uiState.value.phase) {
        is ProductSearchPhase.Results -> phase.products
        is ProductSearchPhase.Unhappy -> phase.alternatives
        else -> emptyList()
    }

    // --- In-place voice capture (mirrors the Home STT flow, reusing the shared domain ports) ---

    private fun startVoiceCapture() {
        val previous = voiceJob
        cancelVoiceJobs()
        phaseBeforeVoice = _uiState.value.phase
        sourceBeforeVoice = _uiState.value.source
        cancelTimers()
        val session = runVoiceSearchSession()
        voiceSession = session
        voiceJob = viewModelScope.launch {
            // The recorder is shared: let a cancelled session release it before this one starts.
            previous?.join()
            session.events.collect(::onVoiceEvent)
        }
    }

    private fun onVoiceEvent(event: VoiceSessionEvent) {
        when (event) {
            is VoiceSessionEvent.Listening -> _uiState.update {
                it.copy(source = SearchSource.Voice, phase = ProductSearchPhase.VoiceListening(event.transcript))
            }
            is VoiceSessionEvent.Finalizing -> onVoiceFinalizing(event.transcript)
            // A successful transcript drives a brand-new in-place search.
            is VoiceSessionEvent.Transcribed -> {
                cancelVoiceSlowResponseTimer()
                performSearch(event.transcript, SearchSource.Voice)
            }
            // No speech or an STT error does NOT trigger an alternative search — restore the previous screen.
            VoiceSessionEvent.NoAudio,
            is VoiceSessionEvent.Failed,
            -> {
                cancelVoiceSlowResponseTimer()
                restorePreVoicePhase()
            }
        }
    }

    /**
     * Recording ended. The first event switches to [ProductSearchPhase.VoiceTranscribing] and starts
     * the slow-response timer; later ones only refresh the heard text (results keep arriving after the
     * recording ends). Once the slow-response message is up it stays.
     */
    private fun onVoiceFinalizing(transcript: String) {
        when (_uiState.value.phase) {
            is ProductSearchPhase.VoiceListening -> {
                _uiState.update { it.copy(phase = ProductSearchPhase.VoiceTranscribing(transcript)) }
                startVoiceSlowResponseTimer()
            }
            is ProductSearchPhase.VoiceTranscribing ->
                _uiState.update { it.copy(phase = ProductSearchPhase.VoiceTranscribing(transcript)) }
            else -> Unit
        }
    }

    /** The consumer tapped the mic while recording: end the recording now and await the final transcript. */
    private fun stopRecording() {
        if (_uiState.value.phase is ProductSearchPhase.VoiceListening) voiceSession?.stop()
    }

    private fun startVoiceSlowResponseTimer() {
        cancelVoiceSlowResponseTimer()
        voiceSlowResponseJob = viewModelScope.launch {
            delay(VoiceSearchConstants.SLOW_RESPONSE_SECONDS * ONE_SECOND_MS)
            _uiState.update {
                if (it.phase is ProductSearchPhase.VoiceTranscribing) {
                    it.copy(phase = ProductSearchPhase.VoiceSlowResponse)
                } else {
                    it
                }
            }
        }
    }

    private fun cancelVoiceSlowResponseTimer() {
        voiceSlowResponseJob?.cancel()
        voiceSlowResponseJob = null
    }

    /**
     * Hidden screen (wave 2, D3: VOZ-004): stop voice capture so the microphone is released. When the
     * screen was opened by voice there is no result to return to, so the exit runs once it is shown
     * again (a navigation while hidden would be dropped).
     */
    private fun onScreenStopped() {
        if (!_uiState.value.isVoiceCapturing) return
        cancelVoiceJobs()
        val restore = phaseBeforeVoice
        if (restore == null) {
            leaveOnResume = true
        } else {
            _uiState.update { it.copy(source = sourceBeforeVoice, phase = restore) }
            if (restore is ProductSearchPhase.Unhappy) startIdleTimer()
        }
    }

    private fun onScreenResumed() {
        if (leaveOnResume) {
            leaveOnResume = false
            emit(ProductSearchUiEffect.NavigateBack)
        }
    }

    private fun cancelVoiceCapture() {
        // Cancelling the session's collection releases the microphone and closes the live stream.
        cancelVoiceJobs()
        restorePreVoicePhase()
    }

    private fun restorePreVoicePhase() {
        // Entered by voice and nothing was heard (or it was cancelled): go back where the consumer came from.
        val restore = phaseBeforeVoice ?: return emit(ProductSearchUiEffect.NavigateBack)
        _uiState.update { it.copy(source = sourceBeforeVoice, phase = restore) }
        if (restore is ProductSearchPhase.Unhappy) startIdleTimer()
    }

    private fun cancelVoiceJobs() {
        voiceJob?.cancel()
        voiceJob = null
        voiceSession = null
        cancelVoiceSlowResponseTimer()
    }

    private fun performSearch(
        query: String,
        source: SearchSource,
        isRefinement: Boolean = false,
        filters: StructuredSearchFilters? = null,
    ) {
        cancelTimers()
        searchJob?.cancel()
        // A fresh (non-refinement) search starts a new results view, so it resets the selection.
        if (!isRefinement) clearSelection()
        _uiState.update {
            it.copy(
                source = source,
                query = "",
                phase = ProductSearchPhase.Searching,
                selectedFilters = selectedFilters.toImmutableSet(),
                selectedSuggestion = selectedSuggestion,
            )
        }
        startSlowSearchTimer()
        searchJob = viewModelScope.launch {
            val result = searchProducts(query, filters = filters)
            slowSearchJob?.cancel()
            result.fold(
                // The backend decides happy vs. unhappy via noResults, and on the unhappy path it
                // already returns up to 3 store suggestions — no second (generalized) request.
                onSuccess = { search ->
                    when {
                        !search.noResults -> settleResults(source, search, query, isRefinement)
                        // Chips that leave no product keep the results view and the chips (F-37).
                        isRefinement && filters != null -> settleNoFilterMatch(source)
                        else -> settleUnhappy(source, search.products, search.suggestedFromCategory)
                    }
                },
                onFailure = { error ->
                    when ((error as? SearchException)?.category ?: SearchErrorCategory.SearchApi) {
                        SearchErrorCategory.Network -> settleNetworkError(source)
                        // A real API error carries no usable suggestions: show the apology alone.
                        SearchErrorCategory.SearchApi -> settleUnhappy(source, emptyList(), null)
                    }
                },
            )
        }
    }

    private fun settleResults(
        source: SearchSource,
        result: ProductSearchResult,
        query: String,
        isRefinement: Boolean,
    ) {
        // A fresh search settling into results fixes the text base and the chip panel for refinements:
        // the backend builds a refined response's panel from the filtered results, which would shrink it.
        if (!isRefinement) {
            baseQuery = query
            baseFilters = result.filters
            baseSuggestions = result.suggestions
        }
        val phase = ProductSearchPhase.Results(
            products = result.products.toImmutableList(),
            displayText = result.displayText,
            filters = baseFilters.toImmutableList(),
            suggestions = baseSuggestions.toImmutableList(),
            missingCategory = result.missingCategory?.takeIf { result.alternativesOnly && it.isNotBlank() },
        )
        settle(source, phase, baseQuery)
    }

    /** The selected chips left no product: stay on results with the chips and say so. */
    private fun settleNoFilterMatch(source: SearchSource) {
        val phase = ProductSearchPhase.Results(
            products = persistentListOf(),
            displayText = "",
            filters = baseFilters.toImmutableList(),
            suggestions = baseSuggestions.toImmutableList(),
            noFilterMatch = true,
        )
        settle(source, phase, baseQuery)
    }

    private fun settleUnhappy(
        source: SearchSource,
        alternatives: List<Product>,
        suggestedFromCategory: String?,
    ) {
        val phase = ProductSearchPhase.Unhappy(
            alternatives = alternatives.take(ProductSearchConstants.ALTERNATIVES_COUNT).toImmutableList(),
            suggestedFromCategory = suggestedFromCategory,
        )
        settle(source, phase, baseQuery)
        startIdleTimer()
    }

    private fun settleNetworkError(source: SearchSource) {
        // A network error is a transient overlay (screen kept) — not a navigable history entry.
        cancelTimers()
        _uiState.update {
            it.copy(source = source, phase = ProductSearchPhase.NetworkError, canGoBack = history.isNotEmpty())
        }
    }

    private fun settle(source: SearchSource, phase: ProductSearchPhase, query: String) {
        history += HistoryEntry(source, phase, query, selectedFilters.toList(), selectedSuggestion)
        _uiState.update {
            it.copy(
                source = source,
                phase = phase,
                canGoBack = history.size > 1,
                selectedFilters = selectedFilters.toImmutableSet(),
                selectedSuggestion = selectedSuggestion,
            )
        }
    }

    private fun clearSelection() {
        selectedFilters.clear()
        selectedSuggestion = null
    }

    private fun onBack() {
        if (_uiState.value.isVoiceCapturing) {
            // Back while the microphone is open cancels the capture, like its cancel control.
            cancelVoiceCapture()
            return
        }
        if (history.size > 1) {
            history.removeAt(history.lastIndex)
            showEntry(history.last())
        } else {
            _uiState.update { it.copy(showCloseConfirmDialog = true) }
        }
    }

    private fun showEntry(entry: HistoryEntry) {
        cancelTimers()
        // History entries are settled snapshots: restore their base query and the chips that were
        // selected for that result (EXP-005). The result itself is not searched again.
        clearSelection()
        selectedFilters.addAll(entry.filters)
        selectedSuggestion = entry.suggestion
        baseQuery = entry.query
        (entry.phase as? ProductSearchPhase.Results)?.let {
            baseFilters = it.filters
            baseSuggestions = it.suggestions
        }
        _uiState.update {
            it.copy(
                source = entry.source,
                phase = entry.phase,
                canGoBack = history.size > 1,
                selectedFilters = selectedFilters.toImmutableSet(),
                selectedSuggestion = selectedSuggestion,
            )
        }
        if (entry.phase is ProductSearchPhase.Unhappy) startIdleTimer()
    }

    private fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query.take(ProductSearchConstants.MAX_QUERY_CHARS)) }
        resetIdleTimerIfUnhappy()
    }

    private fun onSubmitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        performSearch(query, SearchSource.Text)
    }

    private fun goToCatalog() {
        cancelTimers()
        cancelVoiceJobs()
        searchJob?.cancel()
        emit(ProductSearchUiEffect.NavigateToCatalog)
    }

    private fun confirmCloseSearch() {
        history.clear()
        clearSelection()
        baseQuery = ""
        baseFilters = emptyList()
        baseSuggestions = emptyList()
        cancelTimers()
        cancelVoiceJobs()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                showCloseConfirmDialog = false,
                selectedFilters = persistentSetOf(),
                selectedSuggestion = null,
            )
        }
        // End the session first so Home already shows an empty basket (CAR-040).
        viewModelScope.launch {
            endCustomerSession()
            _uiEffect.emit(ProductSearchUiEffect.NavigateHomeIdle)
        }
    }

    private fun startSlowSearchTimer() {
        slowSearchJob?.cancel()
        slowSearchJob = viewModelScope.launch {
            delay(ProductSearchConstants.SLOW_SEARCH_SECONDS * ONE_SECOND_MS)
            _uiState.update {
                if (it.phase is ProductSearchPhase.Searching) it.copy(phase = ProductSearchPhase.SlowSearch) else it
            }
        }
    }

    private fun startIdleTimer() {
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            delay(ProductSearchConstants.UNHAPPY_IDLE_TIMEOUT_SECONDS * ONE_SECOND_MS)
            emit(ProductSearchUiEffect.NavigateHomeIdle)
        }
    }

    private fun resetIdleTimerIfUnhappy() {
        if (_uiState.value.phase is ProductSearchPhase.Unhappy) startIdleTimer()
    }

    private fun cancelTimers() {
        slowSearchJob?.cancel()
        slowSearchJob = null
        idleJob?.cancel()
        idleJob = null
    }

    private fun emit(effect: ProductSearchUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private data class HistoryEntry(
        val source: SearchSource,
        val phase: ProductSearchPhase,
        val query: String,
        val filters: List<SearchFilterOption> = emptyList(),
        val suggestion: String? = null,
    )


    /** Adds to the cart and tells the screen when the 20-product limit refused it (wave 2, D7). */
    private fun addAndReportLimit(item: CartItem) {
        viewModelScope.launch {
            if (addToCart(item) == AddToCartResult.LineLimitReached) emit(ProductSearchUiEffect.LineLimitReached)
        }
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
