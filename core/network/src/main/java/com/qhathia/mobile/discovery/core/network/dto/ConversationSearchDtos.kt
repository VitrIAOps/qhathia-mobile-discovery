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
 */
@Serializable
data class ConversationSearchResponseDto(
    val understood: Boolean = false,
    val results: List<SearchProductDto> = emptyList(),
    val noResults: Boolean = false,
    val displayText: String = "",
    val fallbackMessage: String? = null,
)

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
)
