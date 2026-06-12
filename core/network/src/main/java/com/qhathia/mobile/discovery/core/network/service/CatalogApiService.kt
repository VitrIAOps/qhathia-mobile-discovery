package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.CatalogCategoryDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import javax.inject.Inject

/**
 * Calls `GET /api/v1/catalog/by-category` (authenticated with the Qhathia session JWT) to load the
 * store's available products grouped by category. Applies a per-request 30s timeout, overriding the
 * shared client's longer STT-oriented response timeout.
 */
class CatalogApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun getCatalog(accessToken: String): NetworkResult<List<CatalogCategoryDto>> =
        runCatching {
            client.get("${BuildConfig.QHATHIA_BASE_URL}/api/v1/catalog/by-category") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                timeout { requestTimeoutMillis = CATALOG_REQUEST_TIMEOUT_MS }
            }.body<List<CatalogCategoryDto>>()
        }.toNetworkResult()

    private companion object {
        const val CATALOG_REQUEST_TIMEOUT_MS = 30_000L
    }
}
