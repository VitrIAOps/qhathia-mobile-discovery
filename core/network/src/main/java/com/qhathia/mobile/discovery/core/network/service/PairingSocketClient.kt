package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.dto.PairingStatusMessageDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Raw WebSocket client (ADR-009) for the pairing-session status channel
 * (`WSS /api/v1/devices/pairing-session/{sessionId}`). Exposes each status message the server pushes
 * as a cold [Flow]; the flow completes when the server closes the socket after the terminal message.
 *
 * Production is plain HTTP (no domain/TLS), so the scheme is derived from the configured base URL
 * (`http` → `ws`, `https` → `wss`).
 */
class PairingSocketClient @Inject constructor(
    private val client: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun observe(sessionId: String): Flow<PairingStatusMessageDto> = flow {
        val url = "${BuildConfig.QHATHIA_BASE_URL}$PAIRING_SESSION_PATH/$sessionId"
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        client.webSocket(url) {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    emit(json.decodeFromString<PairingStatusMessageDto>(frame.readText()))
                }
            }
        }
    }

    private companion object {
        const val PAIRING_SESSION_PATH = "/api/v1/devices/pairing-session"
    }
}
