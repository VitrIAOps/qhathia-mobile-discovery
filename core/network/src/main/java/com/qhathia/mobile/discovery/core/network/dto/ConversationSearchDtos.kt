package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/v1/conversations/search`. [transcribedText] carries the query (voice
 * transcript or typed text, ≤500 chars); [sessionId] is optional multi-turn traceability and
 * [language] is an optional ISO 639-1 code (defaults to "es" server-side).
 */
@Serializable
data class ConversationSearchRequestDto(
    val transcribedText: String,
    val sessionId: String? = null,
    val language: String? = null,
)

/**
 * Response of `POST /api/v1/conversations/search`. Only the fields the client renders are modeled;
 * `ignoreUnknownKeys` drops the rest (e.g. `interpretedQuery`, `spokenText`).
 *
 * [filters] and [suggestions] (Fase 9) refine the kiosk results; both are `[]` on the unhappy path.
 * On the unhappy path (`noResults=true`) [results] still carries up to 3 store suggestions and
 * [suggestedFromCategory] (Fase 8) names the category they were drawn from (null when generic).
 * The defaults keep older responses backward-compatible under `ignoreUnknownKeys`.
 */
@Serializable
data class ConversationSearchResponseDto(
    val understood: Boolean = false,
    val results: List<SearchProductDto> = emptyList(),
    val noResults: Boolean = false,
    val suggestedFromCategory: String? = null,
    val displayText: String = "",
    val fallbackMessage: String? = null,
    val filters: List<SearchFilterDto> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

/**
 * A search-refinement criterion and its selectable options (e.g. `criterion = "Voltaje"`,
 * `options = ["12V", "18V", "20V"]`). Plain strings — options carry no count. Max 5 criteria × 5
 * options server-side.
 */
@Serializable
data class SearchFilterDto(
    val criterion: String = "",
    val options: List<String> = emptyList(),
)

/**
 * A search result product.
 * `discountedPrice` is the server-computed price with the active discount applied (`null` = no active
 * discount) and `discountEndsAt` its ISO-8601 end (Fase 25). Not yet documented for
 * this endpoint, so both default to `null` (full price) until the backend sends them.
 */
@Serializable
data class SearchProductDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val availability: String = "",
    val categoryName: String = "",
    @SerialName("primaryImageUrl") val primaryImageUrl: String? = null,
    val relevanceScore: Int = 0,
    val discountedPrice: Double? = null,
    val discountEndsAt: String? = null,
)
