package com.qhathia.mobile.discovery.feature.productsearch.model

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The mutually-exclusive phases of the product-search screen.
 *
 * Processing phases ([Searching], [SlowSearch]) show an animated processing message; settled phases
 * ([Results], [Unhappy], [NetworkError]) show content the consumer can act on.
 */
sealed interface ProductSearchPhase {

    /**
     * Recording the consumer's voice for a new in-place search: "Escuchando…" until words are
     * recognized, then [liveTranscript] (the text recognized so far) in its place.
     */
    data class VoiceListening(val liveTranscript: String = "") : ProductSearchPhase

    /**
     * Recording ended; awaiting the final transcript. [liveTranscript] is the text heard so far (it keeps
     * updating as late results arrive) and replaces "Organizando ideas…", which is shown only while blank.
     */
    data class VoiceTranscribing(val liveTranscript: String = "") : ProductSearchPhase

    /** The transcription response is taking unusually long ("Está tardando…"). */
    data object VoiceSlowResponse : ProductSearchPhase

    /** The catalog query is in flight. */
    data object Searching : ProductSearchPhase

    /** The query has been in flight beyond the slow-search threshold (20s). */
    data object SlowSearch : ProductSearchPhase

    /**
     * The search returned [products]; [displayText] is the results headline. [filters] and
     * [suggestions] drive the refine-by-selection section (both empty hides it); while refining they
     * stay as the fresh search returned them.
     *
     * @property noFilterMatch the selected chips left no product: the grid is replaced by a message and
     *   the chips stay selected (F-37).
     * @property missingCategory the requested product the store does not sell, when the results are
     *   alternatives only (`alternativesOnly`); `null` otherwise.
     */
    data class Results(
        val products: ImmutableList<Product>,
        val displayText: String,
        val filters: ImmutableList<SearchFilter> = persistentListOf(),
        val suggestions: ImmutableList<String> = persistentListOf(),
        val noFilterMatch: Boolean = false,
        val missingCategory: String? = null,
    ) : ProductSearchPhase

    /**
     * No results (the response's `noResults=true`) or a search (API) error. [alternatives] holds up
     * to 3 suggested products the response returned (empty on a search error). [suggestedFromCategory]
     * personalizes the apology's invite line (null → generic invite).
     */
    data class Unhappy(
        val alternatives: ImmutableList<Product>,
        val suggestedFromCategory: String? = null,
    ) : ProductSearchPhase

    /** The search failed due to lost/unstable connectivity; the screen is kept with a connection message. */
    data object NetworkError : ProductSearchPhase
}
