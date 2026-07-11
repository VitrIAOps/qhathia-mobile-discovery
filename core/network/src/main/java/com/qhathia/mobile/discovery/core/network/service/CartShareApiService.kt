package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ShareCartRequestDto
import com.qhathia.mobile.discovery.core.network.dto.ShareCartResponseDto
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
 * Calls `POST /api/v1/carts/share-pdf` (DEVICE-authenticated) to share the cart as a WhatsApp quote
 * PDF. Applies a per-request 30s timeout — the share budget — overriding the shared client's longer
 * response timeout. Returns the share result or a classified [NetworkResult] failure.
 */
class CartShareApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun shareCart(
        accessToken: String,
        request: ShareCartRequestDto,
    ): NetworkResult<ShareCartResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/carts/share-pdf") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
                timeout { requestTimeoutMillis = SHARE_REQUEST_TIMEOUT_MS }
            }.body<ShareCartResponseDto>()
        }.toNetworkResult()

    private companion object {
        // Business rule: ≤30s for a cart-share response (PDF generation + WhatsApp send).
        const val SHARE_REQUEST_TIMEOUT_MS = 30_000L
    }
}
