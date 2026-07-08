package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.CreateOrderRequestDto
import com.qhathia.mobile.discovery.core.network.dto.OrderResponseDto
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
 * Calls `POST /api/v1/orders` (DEVICE-authenticated) to create a purchase order from the cart. Applies
 * a per-request 30s timeout — the order budget — overriding the shared client's longer response
 * timeout. Returns the created order or a classified [NetworkResult] failure.
 */
class OrderApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun createOrder(
        accessToken: String,
        request: CreateOrderRequestDto,
    ): NetworkResult<OrderResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/orders") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
                timeout { requestTimeoutMillis = ORDER_REQUEST_TIMEOUT_MS }
            }.body<OrderResponseDto>()
        }.toNetworkResult()

    private companion object {
        // Business rule: ≤30s for an order-creation response.
        const val ORDER_REQUEST_TIMEOUT_MS = 30_000L
    }
}
