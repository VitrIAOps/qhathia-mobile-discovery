package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ConnectException

class DevicePairingApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(handler: suspend MockRequestHandleScope.() -> HttpResponseData) =
        DevicePairingApiService(
            KtorClientFactory.create(
                MockEngine { req ->
                    captured = req
                    handler()
                },
            ),
        )

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun problem(status: HttpStatusCode, errorCode: String) = service {
        respond("""{"status":${status.value},"errorCode":"$errorCode"}""", status, jsonHeaders)
    }

    @Test
    fun success_returns_device_token() = runTest {
        val result = service {
            respond(
                """{"deviceId":"dev-1","deviceAccessToken":"jwt","storeId":"s","deviceName":"T","platform":"ANDROID","status":"ACTIVE"}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }.exchangeToken("sess-1")

        assertEquals("jwt", (result as PairingExchangeResponse.Success).data.deviceAccessToken)
        assertEquals(HttpMethod.Get, captured!!.method)
        assertTrue(captured!!.url.encodedPath.endsWith("/api/v1/devices/pairing-session/sess-1/token"))
    }

    @Test
    fun http_409_keeps_error_code() = runTest {
        val result = problem(HttpStatusCode.Conflict, "PAIRING_REJECTED").exchangeToken("sess-1")

        assertEquals(PairingExchangeResponse.HttpError(409, "PAIRING_REJECTED"), result)
    }

    @Test
    fun http_410_keeps_error_code() = runTest {
        val result = problem(HttpStatusCode.Gone, "PAIRING_TOKEN_ALREADY_USED").exchangeToken("sess-1")

        assertEquals(PairingExchangeResponse.HttpError(410, "PAIRING_TOKEN_ALREADY_USED"), result)
    }

    @Test
    fun http_error_without_body_has_null_error_code() = runTest {
        val result = service { respond("", HttpStatusCode.NotFound) }.exchangeToken("sess-1")

        assertEquals(PairingExchangeResponse.HttpError(404, null), result)
    }

    @Test
    fun connection_failure_is_a_connectivity_error() = runTest {
        val result = service { throw ConnectException("refused") }.exchangeToken("sess-1")

        assertEquals(NetworkErrorKind.Connectivity, (result as PairingExchangeResponse.NetworkError).kind)
    }
}
