package com.qhathia.mobile.discovery.core.voice.transcription

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Where and how to reach Deepgram's live API. */
data class DeepgramConfig(
    val baseUrl: String,
    val model: String,
    val language: String,
)

/**
 * Builds a Deepgram [LiveTranscriber] over the host app's [client], which must have the WebSockets
 * plugin installed.
 */
fun deepgramLiveTranscriber(client: HttpClient, config: DeepgramConfig): LiveTranscriber =
    DeepgramLiveTranscriber(client, config)

/**
 * Streams microphone audio to Deepgram's live `/v1/listen` WebSocket (linear16, 16 kHz, mono, with
 * interim results) and exposes each `Results` message.
 *
 * Every chunk of [stream]'s `audio` goes out as a binary frame; when `audio` completes the client
 * sends `CloseStream`, Deepgram flushes the last results and closes the socket, and the returned flow
 * completes. Any other ending fails the flow with a [VoiceStreamException].
 *
 * Auth uses the `Token` scheme with an ephemeral credential. The token is never logged.
 */
internal class DeepgramLiveTranscriber(
    private val client: HttpClient,
    config: DeepgramConfig,
) : LiveTranscriber {

    internal val listenUrl: String = config.baseUrl
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") +
        "/v1/listen?model=${config.model}&language=${config.language}" +
        "&encoding=linear16&sample_rate=16000&channels=1" +
        "&interim_results=true&smart_format=true&punctuate=true"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    override fun stream(audio: Flow<ByteArray>, token: String): Flow<TranscriptResult> =
        rawStream(audio, token).map { TranscriptResult(it.transcript, it.isFinal) }

    private fun rawStream(audio: Flow<ByteArray>, token: String): Flow<DeepgramLiveResultDto> = channelFlow {
        try {
            client.webSocket(
                urlString = listenUrl,
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
                    throw VoiceStreamException(closeCode = reason.code.toInt(), message = reason.message)
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: VoiceStreamException) {
            throw failure
        } catch (failure: WebSocketException) {
            // Handshake rejected (e.g. 401 for an invalid/expired token, 400 for bad params).
            throw VoiceStreamException(handshakeRejected = true, message = failure.message, cause = failure)
        } catch (failure: Exception) {
            throw VoiceStreamException(
                transport = failure.toVoiceTransportFailure(),
                message = failure.message,
                cause = failure,
            )
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
    }
}

/** Classifies a transport failure; matches how the host's HTTP layer reads the same exceptions. */
fun Throwable.toVoiceTransportFailure(): VoiceTransportFailure = when (this) {
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException,
    -> VoiceTransportFailure.Timeout

    is UnknownHostException,
    is ConnectException,
    is IOException,
    -> VoiceTransportFailure.Connectivity

    else -> VoiceTransportFailure.Unknown
}
