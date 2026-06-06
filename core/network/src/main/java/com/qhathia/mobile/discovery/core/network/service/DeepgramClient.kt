package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeepgramListenResponseDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject

/**
 * Posts recorded audio to Deepgram's pre-recorded `/v1/listen` endpoint. The audio is sent as the
 * raw request body (base64/JSON wrappers are rejected); the codec is inferred by Deepgram from the
 * file headers, so no `encoding` param is sent.
 *
 * Auth scheme is centralized in [AUTH_SCHEME]: the ephemeral credential from `/api/v1/stt/token`
 * is API-key shaped, so `Token` is used. Flip to `Bearer` here if a live token is a granted JWT.
 */
class DeepgramClient @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun transcribe(
        audio: ByteArray,
        mimeType: String,
        token: String,
    ): NetworkResult<DeepgramListenResponseDto> =
        runCatching {
            client.post("${BuildConfig.DEEPGRAM_BASE_URL}/v1/listen") {
                url {
                    parameters.append("model", "nova-3")
                    parameters.append("smart_format", "true")
                    parameters.append("language", "es")
                    parameters.append("punctuate", "true")
                }
                header(HttpHeaders.Authorization, "$AUTH_SCHEME $token")
                contentType(ContentType.parse(mimeType))
                setBody(audio)
            }.body<DeepgramListenResponseDto>()
        }.toNetworkResult()

    private companion object {
        const val AUTH_SCHEME = "Token"
    }
}
