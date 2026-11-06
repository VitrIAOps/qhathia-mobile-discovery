package com.qhathia.mobile.discovery.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the single Ktor [HttpClient] for the app. Per-phase budgets live here (connect ≤10s,
 * socket ≤60s for uploads, request ≤120s for the transcription response); the 150s end-to-end cap
 * is enforced with an outer `withTimeout` in the use case, since it spans non-HTTP phases.
 *
 * Services issue absolute URLs (Qhathia and Deepgram live on different hosts), so no `defaultRequest`
 * base URL is set — only the JSON content type and a debug-gated body logger.
 *
 * Every `401` with `errorCode: DEVICE_UNLINKED` (api-contract §35), from any service, is reported to
 * `onDeviceUnlinked` with the request's bearer token. The error still reaches the caller unchanged.
 */
internal object KtorClientFactory {

    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val UPLOAD_SOCKET_TIMEOUT_MS = 60_000L
    private const val RESPONSE_TIMEOUT_MS = 120_000L
    private const val DEVICE_UNLINKED = "DEVICE_UNLINKED"
    private const val BEARER_PREFIX = "Bearer "

    fun create(enableLogging: Boolean, onDeviceUnlinked: (bearerToken: String?) -> Unit = {}): HttpClient =
        HttpClient(CIO) { configure(enableLogging, onDeviceUnlinked) }

    /** Builds a client over [engine] with the production configuration (used by tests with a mock engine). */
    fun create(
        engine: HttpClientEngine,
        enableLogging: Boolean = false,
        onDeviceUnlinked: (bearerToken: String?) -> Unit = {},
    ): HttpClient = HttpClient(engine) { configure(enableLogging, onDeviceUnlinked) }

    private fun HttpClientConfig<*>.configure(
        enableLogging: Boolean,
        onDeviceUnlinked: (bearerToken: String?) -> Unit,
    ) {
        expectSuccess = true
        HttpResponseValidator {
            // Observes only: the exception is rethrown by Ktor, so each caller still gets its HttpError.
            handleResponseExceptionWithRequest { cause, request ->
                val response = (cause as? ClientRequestException)?.response ?: return@handleResponseExceptionWithRequest
                if (response.status == HttpStatusCode.Unauthorized &&
                    response.readProblemDetail()?.resolvedErrorCode == DEVICE_UNLINKED
                ) {
                    onDeviceUnlinked(request.headers[HttpHeaders.Authorization]?.removePrefix(BEARER_PREFIX))
                }
            }
        }
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
                // Personal data and secrets never reach logcat (design D10, RED-060). The level stays
                // BODY so QA still sees the shape of each call.
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorClient", LogRedactor.redact(message))
                    }
                }
                level = LogLevel.BODY
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
    }
}
