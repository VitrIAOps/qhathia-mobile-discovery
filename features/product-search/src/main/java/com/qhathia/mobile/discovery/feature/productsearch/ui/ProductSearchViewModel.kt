package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants
import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.usecase.GeneralizeQueryUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SearchProductsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.TranscribeVoiceSearchUseCase
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEffect
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiState
import com.qhathia.mobile.discovery.feature.productsearch.navigation.ARG_QUERY
import com.qhathia.mobile.discovery.feature.productsearch.navigation.ARG_SOURCE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
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
import javax.inject.Inject

/**
 * Drives the product-search screen and owns its state machine:
 *
 * `Searching → (SlowSearch) → Results` on success; on no-results or a search (API) error,
 * `→ SearchingAlternatives → Unhappy` (a disguised alternative search derived from the keyword,
 * keeping its first 3 products); a connectivity failure settles on `NetworkError` (screen kept).
 * Each settled outcome is pushed onto an in-memory history so Volver can return to the previous
 * result; with no previous result Volver opens the close-confirmation dialog. Nueva búsqueda (once
 * confirmed) clears the history and returns Home idle, as does the 30s idle timeout on the unhappy
 * screen. The microphone animation is retained while processing only for a voice-initiated search.
 */
@HiltViewModel
class ProductSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchProducts: SearchProductsUseCase,
    private val generalizeQuery: GeneralizeQueryUseCase,
    private val audioRecorder: AudioRecorder,
    private val transcribeVoiceSearch: TranscribeVoiceSearchUseCase,
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

    private var searchJob: Job? = null
    private var slowSearchJob: Job? = null
    private var idleJob: Job? = null

    private var voiceJob: Job? = null
    private var transcribeJob: Job? = null

    /** Guards against the listening loop and a user stop both starting transcription. */
    private var voiceFinishing = false

    /** The phase/source to restore if an in-place voice capture is cancelled or yields no usable input. */
    private var phaseBeforeVoice: ProductSearchPhase? = null
    private var sourceBeforeVoice: SearchSource = SearchSource.Text

    init {
        performSearch(initialQuery, initialSource)
    }

    fun onEvent(event: ProductSearchUiEvent) {
        when (event) {
            ProductSearchUiEvent.Back -> onBack()
            ProductSearchUiEvent.NewSearch -> _uiState.update { it.copy(showCloseConfirmDialog = true) }
            ProductSearchUiEvent.ConfirmCloseSearch -> confirmCloseSearch()
            ProductSearchUiEvent.DismissDialog -> _uiState.update { it.copy(showCloseConfirmDialog = false) }
            is ProductSearchUiEvent.QueryChanged -> onQueryChanged(event.query)
            ProductSearchUiEvent.SubmitSearch -> onSubmitSearch()
            ProductSearchUiEvent.ActivateMic -> startVoiceCapture()
            ProductSearchUiEvent.StopRecording -> beginTranscription()
            ProductSearchUiEvent.CancelVoice -> cancelVoiceCapture()
            is ProductSearchUiEvent.CartTapped -> resetIdleTimerIfUnhappy() // cart navigation out of scope
            is ProductSearchUiEvent.ProductTapped -> openDetail(event.productId)
        }
    }

    private fun openDetail(productId: String) {
        resetIdleTimerIfUnhappy()
        emit(ProductSearchUiEffect.NavigateToDetail(productId))
    }

    // --- In-place voice capture (mirrors the Home STT flow, reusing the shared domain ports) ---

    private fun startVoiceCapture() {
        cancelVoiceJobs()
        voiceFinishing = false
        phaseBeforeVoice = _uiState.value.phase
        sourceBeforeVoice = _uiState.value.source
        cancelTimers()
        voiceJob = viewModelScope.launch {
            audioRecorder.start()
            _uiState.update { it.copy(source = SearchSource.Voice, phase = ProductSearchPhase.VoiceListening) }

            val pollMs = VoiceSearchConstants.LEVEL_POLL_INTERVAL_MS
            val inputWaitMs = VoiceSearchConstants.INPUT_WAIT_SECONDS * ONE_SECOND_MS
            val maxRecordingMs = VoiceSearchConstants.MAX_RECORDING_SECONDS * ONE_SECOND_MS

            var elapsedMs = 0L
            var hasSpoken = false
            var trailingSilenceMs = 0L

            while (true) {
                delay(pollMs)
                elapsedMs += pollMs
                when (audioRecorder.pollLevel()) {
                    AudioLevel.Speech -> { hasSpoken = true; trailingSilenceMs = 0 }
                    AudioLevel.Silence -> if (hasSpoken) trailingSilenceMs += pollMs
                }
                if (hasSpoken && trailingSilenceMs >= VoiceSearchConstants.SILENCE_AUTO_STOP_MS) {
                    beginTranscription(); return@launch
                }
                if (!hasSpoken && elapsedMs >= inputWaitMs) {
                    abandonVoiceCapture(); return@launch
                }
                if (elapsedMs >= maxRecordingMs) {
                    beginTranscription(); return@launch
                }
            }
        }
    }

    private fun beginTranscription() {
        if (voiceFinishing) return
        if (_uiState.value.phase != ProductSearchPhase.VoiceListening) return
        voiceFinishing = true
        voiceJob?.cancel()
        voiceJob = null
        transcribeJob = viewModelScope.launch {
            val audio = audioRecorder.stop()
            if (audio == null) {
                voiceFinishing = false
                restorePreVoicePhase()
                return@launch
            }
            _uiState.update { it.copy(phase = ProductSearchPhase.VoiceTranscribing) }
            val slowJob = launch {
                delay(VoiceSearchConstants.SLOW_RESPONSE_SECONDS * ONE_SECOND_MS)
                _uiState.update {
                    if (it.phase is ProductSearchPhase.VoiceTranscribing) {
                        it.copy(phase = ProductSearchPhase.VoiceSlowResponse)
                    } else {
                        it
                    }
                }
            }
            val result = transcribeVoiceSearch(audio)
            slowJob.cancel()
            voiceFinishing = false
            result.fold(
                // A successful transcript drives a brand-new in-place search.
                onSuccess = { performSearch(it.transcript, SearchSource.Voice) },
                // STT errors do NOT trigger an alternative search — restore the previous screen.
                onFailure = { restorePreVoicePhase() },
            )
        }
    }

    private fun abandonVoiceCapture() {
        if (voiceFinishing) return
        voiceFinishing = true
        transcribeJob = viewModelScope.launch {
            audioRecorder.stop()
            voiceFinishing = false
            restorePreVoicePhase()
        }
    }

    private fun cancelVoiceCapture() {
        cancelVoiceJobs()
        voiceFinishing = false
        audioRecorder.cancel()
        restorePreVoicePhase()
    }

    private fun restorePreVoicePhase() {
        val restore = phaseBeforeVoice ?: ProductSearchPhase.NetworkError
        _uiState.update { it.copy(source = sourceBeforeVoice, phase = restore) }
        if (restore is ProductSearchPhase.Unhappy) startIdleTimer()
    }

    private fun cancelVoiceJobs() {
        voiceJob?.cancel()
        voiceJob = null
        transcribeJob?.cancel()
        transcribeJob = null
    }

    private fun performSearch(query: String, source: SearchSource) {
        cancelTimers()
        searchJob?.cancel()
        _uiState.update {
            it.copy(source = source, query = "", phase = ProductSearchPhase.Searching)
        }
        startSlowSearchTimer()
        searchJob = viewModelScope.launch {
            val result = searchProducts(query)
            slowSearchJob?.cancel()
            result.fold(
                onSuccess = { search ->
                    if (search.products.isNotEmpty()) settleResults(source, search)
                    else startAlternativeSearch(query, source)
                },
                onFailure = { error ->
                    when ((error as? SearchException)?.category ?: SearchErrorCategory.SearchApi) {
                        SearchErrorCategory.Network -> settleNetworkError(source)
                        SearchErrorCategory.SearchApi -> startAlternativeSearch(query, source)
                    }
                },
            )
        }
    }

    private suspend fun startAlternativeSearch(originalQuery: String, source: SearchSource) {
        _uiState.update { it.copy(phase = ProductSearchPhase.SearchingAlternatives) }
        val generalized = generalizeQuery(originalQuery)
        val alternatives = searchProducts(generalized).getOrNull()
            ?.products
            ?.take(ProductSearchConstants.ALTERNATIVES_COUNT)
            .orEmpty()
        settleUnhappy(source, alternatives)
    }

    private fun settleResults(source: SearchSource, result: ProductSearchResult) {
        val phase = ProductSearchPhase.Results(result.products.toImmutableList(), result.displayText)
        settle(source, phase)
    }

    private fun settleUnhappy(source: SearchSource, alternatives: List<Product>) {
        settle(source, ProductSearchPhase.Unhappy(alternatives.toImmutableList()))
        startIdleTimer()
    }

    private fun settleNetworkError(source: SearchSource) {
        // A network error is a transient overlay (screen kept) — not a navigable history entry.
        cancelTimers()
        _uiState.update {
            it.copy(source = source, phase = ProductSearchPhase.NetworkError, canGoBack = history.isNotEmpty())
        }
    }

    private fun settle(source: SearchSource, phase: ProductSearchPhase) {
        history += HistoryEntry(source, phase)
        _uiState.update {
            it.copy(source = source, phase = phase, canGoBack = history.size > 1)
        }
    }

    private fun onBack() {
        if (history.size > 1) {
            history.removeAt(history.lastIndex)
            showEntry(history.last())
        } else {
            _uiState.update { it.copy(showCloseConfirmDialog = true) }
        }
    }

    private fun showEntry(entry: HistoryEntry) {
        cancelTimers()
        _uiState.update {
            it.copy(source = entry.source, phase = entry.phase, canGoBack = history.size > 1)
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

    private fun confirmCloseSearch() {
        history.clear()
        cancelTimers()
        cancelVoiceJobs()
        searchJob?.cancel()
        _uiState.update { it.copy(showCloseConfirmDialog = false) }
        emit(ProductSearchUiEffect.NavigateHomeIdle)
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

    private data class HistoryEntry(val source: SearchSource, val phase: ProductSearchPhase)

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
