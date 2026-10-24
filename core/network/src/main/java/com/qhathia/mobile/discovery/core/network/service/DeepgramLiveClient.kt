package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.dto.DeepgramLiveResultDto
import com.qhathia.mobile.discovery.core.network.toNetworkErrorKind
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Streams microphone audio to Deepgram's live `/v1/listen` WebSocket (linear16, 16 kHz, mono, with
 * interim results) and exposes each `Results` message.
 *
 * Every chunk of [stream]'s `audio` goes out as a binary frame; when `audio` completes the client
 * sends `CloseStream`, Deepgram flushes the last results and closes the socket, and the returned flow
 * completes. Any other ending fails the flow with a [DeepgramStreamException].
 *
 * Auth uses the `Token` scheme with the ephemeral credential from `/api/v1/stt/token`. The token is
 * never logged.
 */
class DeepgramLiveClient @Inject constructor(
    private val client: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    fun stream(audio: Flow<ByteArray>, token: String): Flow<DeepgramLiveResultDto> = channelFlow {
        try {
            client.webSocket(
                urlString = LISTEN_URL,
                request = { header(HttpHeaders.Authorization, "$AUTH_SCHEME $token") },
            ) {
                val sender = launch {
                    audio.collect { chunk -> send(Frame.Binary(true, chunk)) }
                    send(Frame.Text(CLOSE_STREAM_MESSAGE))
                }
                for (frame in incoming) {
                    if (frame is Frame.Text) decodeResult(frame.readText())?.let { this@channelFlow.send(it) }
                }
                sender.cancel()
                val reason = closeReason.await()
                if (reason != null && reason.code != CloseReason.Codes.NORMAL.code) {
                    throw DeepgramStreamException(closeCode = reason.code.toInt(), message = reason.message)
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: DeepgramStreamException) {
            throw failure
        } catch (failure: WebSocketException) {
            // Handshake rejected (e.g. 401 for an invalid/expired token, 400 for bad params).
            throw DeepgramStreamException(handshakeRejected = true, message = failure.message, cause = failure)
        } catch (failure: Exception) {
            throw DeepgramStreamException(kind = failure.toNetworkErrorKind(), message = failure.message, cause = failure)
        }
    }

    /** Decodes a `Results` message, or returns `null` for any other message type. */
    internal fun decodeResult(text: String): DeepgramLiveResultDto? {
        val type = runCatching { json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.content }
            .getOrNull()
        return if (type == RESULTS_TYPE) runCatching { json.decodeFromString<DeepgramLiveResultDto>(text) }.getOrNull() else null
    }

    private companion object {
        const val AUTH_SCHEME = "Token"
        const val RESULTS_TYPE = "Results"
        const val CLOSE_STREAM_MESSAGE = """{"type":"CloseStream"}"""

        val LISTEN_URL: String = BuildConfig.DEEPGRAM_BASE_URL
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
            "/v1/listen?model=nova-3&language=es&encoding=linear16&sample_rate=16000&channels=1" +
            "&interim_results=true&smart_format=true&punctuate=true"
    }
}

/**
 * Why a live transcription stream failed. Exactly one of [closeCode] (Deepgram closed the socket
 * with a non-normal code), [handshakeRejected] (the upgrade was refused) or [kind] (transport
 * failure) describes the cause.
 */
class DeepgramStreamException(
    val closeCode: Int? = null,
    val handshakeRejected: Boolean = false,
    val kind: NetworkErrorKind? = null,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: "Deepgram live stream failed", cause)
