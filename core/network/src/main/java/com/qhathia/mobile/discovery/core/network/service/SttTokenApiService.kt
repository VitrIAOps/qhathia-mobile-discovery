package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.SttTokenResponseDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import javax.inject.Inject

/**
 * Calls `POST /api/v1/stt/token` (authenticated with the Qhathia session JWT) to obtain a
 * short-lived Deepgram credential. The real Deepgram API key never reaches the client.
 */
class SttTokenApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun getToken(accessToken: String): NetworkResult<SttTokenResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/stt/token") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }.body<SttTokenResponseDto>()
        }.toNetworkResult()
}
