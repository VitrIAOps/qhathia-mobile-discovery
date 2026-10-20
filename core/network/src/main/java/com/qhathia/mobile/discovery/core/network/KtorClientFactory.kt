package com.qhathia.mobile.discovery.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the single Ktor [HttpClient] for the app. Per-phase budgets live here (connect ≤10s,
 * socket ≤60s for uploads, request ≤120s for the transcription response); the 150s end-to-end cap
 * is enforced with an outer `withTimeout` in the use case, since it spans non-HTTP phases.
 *
 * Services issue absolute URLs (Qhathia and Deepgram live on different hosts), so no `defaultRequest`
 * base URL is set — only the JSON content type and a debug-gated body logger.
 */
internal object KtorClientFactory {

    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val UPLOAD_SOCKET_TIMEOUT_MS = 60_000L
    private const val RESPONSE_TIMEOUT_MS = 120_000L

    fun create(enableLogging: Boolean): HttpClient = HttpClient(CIO) { configure(enableLogging) }

    /** Builds a client over [engine] with the production configuration (used by tests with a mock engine). */
    fun create(engine: HttpClientEngine, enableLogging: Boolean = false): HttpClient =
        HttpClient(engine) { configure(enableLogging) }

    private fun HttpClientConfig<*>.configure(enableLogging: Boolean) {
        expectSuccess = true
        // Raw WebSocket support (ADR-009) for the device pairing-session status channel.
        install(WebSockets)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = UPLOAD_SOCKET_TIMEOUT_MS
            requestTimeoutMillis = RESPONSE_TIMEOUT_MS
        }
        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorClient", message)
                    }
                }
                level = LogLevel.BODY
            }
        }
    }
}
