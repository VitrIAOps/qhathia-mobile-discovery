package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ConversationSearchRequestDto
import com.qhathia.mobile.discovery.core.network.dto.ConversationSearchResponseDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject

/**
 * Calls `POST /api/v1/conversations/search` (authenticated with the Qhathia session JWT) to search
 * the store catalog. Applies a per-request 30s timeout — the business search budget — overriding the
 * shared client's longer STT-oriented response timeout.
 */
class ConversationSearchApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun search(
        accessToken: String,
        query: String,
        language: String,
        sessionId: String? = null,
    ): NetworkResult<ConversationSearchResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/conversations/search") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(
                    ConversationSearchRequestDto(
                        transcribedText = query,
                        sessionId = sessionId,
                        language = language,
                    ),
                )
                timeout { requestTimeoutMillis = SEARCH_REQUEST_TIMEOUT_MS }
            }.body<ConversationSearchResponseDto>()
        }.toNetworkResult()

    private companion object {
        // Business rule: ≤30s for a search response.
        const val SEARCH_REQUEST_TIMEOUT_MS = 30_000L
    }
}
