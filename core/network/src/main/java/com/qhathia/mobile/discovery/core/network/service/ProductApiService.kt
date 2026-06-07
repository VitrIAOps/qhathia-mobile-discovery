package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ProductDetailResponseDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import javax.inject.Inject

/**
 * Calls `GET /api/v1/products/{id}` (authenticated with the Qhathia session JWT) to fetch a single
 * product's full detail. Applies a per-request 30s timeout — the business detail budget — overriding
 * the shared client's longer STT-oriented response timeout.
 */
class ProductApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun getProduct(
        accessToken: String,
        id: String,
    ): NetworkResult<ProductDetailResponseDto> =
        runCatching {
            client.get("${BuildConfig.QHATHIA_BASE_URL}/api/v1/products/$id") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                timeout { requestTimeoutMillis = DETAIL_REQUEST_TIMEOUT_MS }
            }.body<ProductDetailResponseDto>()
        }.toNetworkResult()

    private companion object {
        // Business rule: ≤30s for a detail response.
        const val DETAIL_REQUEST_TIMEOUT_MS = 30_000L
    }
}
