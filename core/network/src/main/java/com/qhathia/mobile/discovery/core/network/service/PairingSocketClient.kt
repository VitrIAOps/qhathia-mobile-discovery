package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.dto.PairingStatusMessageDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Thrown when the server refuses the WebSocket upgrade (any status other than 101). The server does
 * this when the session does not exist or has expired, so the caller can ask the exchange endpoint
 * what happened instead of reconnecting forever.
 */
class PairingHandshakeRejectedException(message: String?, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Raw WebSocket client (ADR-009) for the pairing-session status channel
 * (`WSS /api/v1/devices/pairing-session/{sessionId}`). Exposes each status message the server pushes
 * as a cold [Flow]; the flow completes when the server closes the socket.
 *
 * A ping is sent every [PING_INTERVAL_MS]. The server's pong counts as traffic, so the client's 60 s
 * socket timeout never closes an idle wait (task 1.2: without it the socket closed at 60 s). The ping
 * lives here, not on the shared `WebSockets` plugin, so the Deepgram voice socket is not affected.
 *
 * A refused upgrade fails the flow with [PairingHandshakeRejectedException]; any failure after the
 * socket opened fails it with the original exception.
 *
 * Production is plain HTTP (no domain/TLS), so the scheme is derived from the configured base URL
 * (`http` → `ws`, `https` → `wss`).
 */
class PairingSocketClient @Inject constructor(
    private val client: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun observe(sessionId: String): Flow<PairingStatusMessageDto> = channelFlow {
        val url = "${BuildConfig.QHATHIA_BASE_URL}$PAIRING_SESSION_PATH/$sessionId"
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        var opened = false
        try {
            client.webSocket(url) {
                opened = true
                val pinger = launch {
                    while (isActive) {
                        delay(PING_INTERVAL_MS)
                        try {
                            outgoing.send(Frame.Ping(ByteArray(0)))
                        } catch (_: ClosedSendChannelException) {
                            break
                        }
                    }
                }
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        send(json.decodeFromString<PairingStatusMessageDto>(frame.readText()))
                    }
                }
                pinger.cancel()
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: WebSocketException) {
            if (opened) throw failure
            throw PairingHandshakeRejectedException(failure.message, failure)
        } catch (failure: ResponseException) {
            if (opened) throw failure
            throw PairingHandshakeRejectedException(failure.message, failure)
        }
    }

    private companion object {
        const val PAIRING_SESSION_PATH = "/api/v1/devices/pairing-session"
        const val PING_INTERVAL_MS = 20_000L
    }
}
