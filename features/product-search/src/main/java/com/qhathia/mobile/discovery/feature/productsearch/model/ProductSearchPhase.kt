package com.qhathia.mobile.discovery.feature.productsearch.model

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The mutually-exclusive phases of the product-search screen.
 *
 * Processing phases ([Searching], [SlowSearch], [SearchingAlternatives]) show an animated processing
 * message; settled phases ([Results], [Unhappy], [NetworkError]) show content the consumer can act on.
 */
sealed interface ProductSearchPhase {

    /** Recording the consumer's voice for a new in-place search ("Escuchando…"). */
    data object VoiceListening : ProductSearchPhase

    /** Uploading/transcribing the captured audio ("Organizando ideas…"). */
    data object VoiceTranscribing : ProductSearchPhase

    /** The transcription response is taking unusually long ("Está tardando…"). */
    data object VoiceSlowResponse : ProductSearchPhase

    /** The catalog query is in flight. */
    data object Searching : ProductSearchPhase

    /** The query has been in flight beyond the slow-search threshold (20s). */
    data object SlowSearch : ProductSearchPhase

    /** A disguised alternative search is running after a no-results / search-error outcome. */
    data object SearchingAlternatives : ProductSearchPhase

    /**
     * The search returned [products]; [displayText] is the results headline. [filters] and
     * [suggestions] drive the refine-by-selection section (both empty hides it).
     */
    data class Results(
        val products: ImmutableList<Product>,
        val displayText: String,
        val filters: ImmutableList<SearchFilter> = persistentListOf(),
        val suggestions: ImmutableList<String> = persistentListOf(),
    ) : ProductSearchPhase

    /** No results or a search (API) error; [alternatives] holds up to 3 suggested products. */
    data class Unhappy(val alternatives: ImmutableList<Product>) : ProductSearchPhase

    /** The search failed due to lost/unstable connectivity; the screen is kept with a connection message. */
    data object NetworkError : ProductSearchPhase
}
