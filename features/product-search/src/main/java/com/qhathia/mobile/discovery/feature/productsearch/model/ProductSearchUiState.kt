package com.qhathia.mobile.discovery.feature.productsearch.model

import com.qhathia.mobile.discovery.core.domain.model.SearchSource

/**
 * Immutable snapshot of the product-search screen.
 *
 * @property source where the active search originated (drives mic-animation retention).
 * @property query the current bottom search-bar text for the next query.
 * @property phase the current screen phase.
 * @property canGoBack whether a previous result exists to return to (Volver).
 * @property showCloseConfirmDialog whether the close-current-search confirmation dialog is visible.
 */
data class ProductSearchUiState(
    val source: SearchSource = SearchSource.Text,
    val query: String = "",
    val phase: ProductSearchPhase = ProductSearchPhase.Searching,
    val canGoBack: Boolean = false,
    val showCloseConfirmDialog: Boolean = false,
) {
    /** True while voice is being captured or a search (original or alternative) is in flight. */
    val isProcessing: Boolean
        get() = phase is ProductSearchPhase.VoiceListening ||
            phase is ProductSearchPhase.VoiceTranscribing ||
            phase is ProductSearchPhase.VoiceSlowResponse ||
            phase is ProductSearchPhase.Searching ||
            phase is ProductSearchPhase.SlowSearch ||
            phase is ProductSearchPhase.SearchingAlternatives

    /** True while the consumer's voice is being captured or transcribed in-place. */
    val isVoiceCapturing: Boolean
        get() = phase is ProductSearchPhase.VoiceListening ||
            phase is ProductSearchPhase.VoiceTranscribing ||
            phase is ProductSearchPhase.VoiceSlowResponse

    /**
     * The mic animation is retained while processing for a voice-initiated search (always while
     * capturing voice); a text search shows no mic animation while searching (business rule).
     */
    val showProcessingMicAnimation: Boolean
        get() = isProcessing && (isVoiceCapturing || source == SearchSource.Voice)
}
