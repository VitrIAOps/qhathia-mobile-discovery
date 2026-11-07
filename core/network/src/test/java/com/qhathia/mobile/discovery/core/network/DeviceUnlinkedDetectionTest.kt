package com.qhathia.mobile.discovery.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceUnlinkedDetectionTest {

    private val reported = mutableListOf<String?>()

    private suspend fun call(status: HttpStatusCode, body: String, token: String? = "device-jwt"): NetworkResult<String> {
        val client = KtorClientFactory.create(
            engine = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/problem+json")) },
            onDeviceUnlinked = { reported += it },
        )
        return runCatching {
            client.get("https://api.test/x") { token?.let { bearerAuth(it) } }.bodyAsText()
        }.toNetworkResult()
    }

    @Test
    fun device_unlinked_401_is_reported_with_token_and_still_reaches_caller() = runTest {
        val result = call(HttpStatusCode.Unauthorized, UNLINKED_BODY)

        assertEquals(listOf<String?>("device-jwt"), reported)
        val error = result as NetworkResult.HttpError
        assertEquals(401, error.code)
        // The validator already read the body; the caller can still parse it.
        assertEquals("DEVICE_UNLINKED", error.errorCode)
    }

    @Test
    fun request_without_bearer_reports_null_token() = runTest {
        call(HttpStatusCode.Unauthorized, UNLINKED_BODY, token = null)

        assertEquals(listOf<String?>(null), reported)
    }

    @Test
    fun forbidden_is_not_reported() = runTest {
        call(HttpStatusCode.Forbidden, """{"status":403,"errorCode":"FORBIDDEN"}""")

        assertTrue(reported.isEmpty())
    }

    @Test
    fun bare_401_is_not_reported() = runTest {
        call(HttpStatusCode.Unauthorized, "")

        assertTrue(reported.isEmpty())
    }

    @Test
    fun other_401_code_is_not_reported() = runTest {
        call(HttpStatusCode.Unauthorized, """{"status":401,"errorCode":"SESSION_REVOKED"}""")

        assertTrue(reported.isEmpty())
    }

    @Test
    fun success_is_not_reported() = runTest {
        call(HttpStatusCode.OK, "{}")

        assertTrue(reported.isEmpty())
    }

    @Test
    fun signal_emits_to_collectors() = runTest {
        val signal = DeviceUnlinkedSignal()
        val received = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { signal.events.collect { received += it } }

        signal.notifyUnlinked("t1")

        assertEquals(listOf<String?>("t1"), received)
    }

    private companion object {
        const val UNLINKED_BODY =
            """{"type":"/errors/device-unlinked","title":"Device Unlinked","status":401,""" +
                """"detail":"Esta tablet fue desvinculada de la tienda.","errorCode":"DEVICE_UNLINKED"}"""
    }
}
