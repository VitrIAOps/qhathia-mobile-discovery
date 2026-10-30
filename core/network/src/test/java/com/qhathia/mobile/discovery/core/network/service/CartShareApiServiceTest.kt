package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ShareCartItemRequestDto
import com.qhathia.mobile.discovery.core.network.dto.ShareCartRequestDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestTimeoutException
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
import java.net.UnknownHostException

class CartShareApiServiceTest {

    private var captured: HttpRequestData? = null
    private var requests = 0

    private val request = ShareCartRequestDto(
        customerName = "Ana",
        customerPhone = "987654321",
        items = listOf(ShareCartItemRequestDto(productId = "p1", quantity = 2)),
    )

    private fun service(handler: suspend MockRequestHandleScope.() -> HttpResponseData) =
        CartShareApiService(
            KtorClientFactory.create(
                MockEngine { req ->
                    captured = req
                    requests++
                    handler()
                },
            ),
        )

    private fun body(status: String) =
        """{"status":"$status","messageId":null,"pdfUrl":"https://r2.dev/c.pdf","error":"Account not registered"}"""

    private fun json(status: String) = service {
        respond(body(status), HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun posts_to_share_pdf_with_device_token() = runTest {
        json("SENT").shareCart("device-jwt", request)

        val sent = captured!!
        assertEquals(HttpMethod.Post, sent.method)
        assertTrue(sent.url.encodedPath.endsWith("/api/v1/carts/share-pdf"))
        assertEquals("Bearer device-jwt", sent.headers[HttpHeaders.Authorization])
    }

    @Test
    fun parses_201_failed_body_as_success_with_status() = runTest {
        val result = json("FAILED").shareCart("t", request)

        val dto = (result as NetworkResult.Success).data
        assertEquals("FAILED", dto.status)
        assertEquals("https://r2.dev/c.pdf", dto.pdfUrl)
    }

    @Test
    fun maps_400_to_http_error_with_one_request() = runTest {
        val result = service { respondError(HttpStatusCode.BadRequest) }.shareCart("t", request)

        assertEquals(400, (result as NetworkResult.HttpError).code)
        assertEquals(1, requests)
    }

    @Test
    fun maps_422_to_http_error() = runTest {
        val result = service { respondError(HttpStatusCode.UnprocessableEntity) }.shareCart("t", request)

        assertEquals(422, (result as NetworkResult.HttpError).code)
    }

    @Test
    fun maps_500_to_http_error() = runTest {
        val result = service { respondError(HttpStatusCode.InternalServerError) }.shareCart("t", request)

        assertEquals(500, (result as NetworkResult.HttpError).code)
    }

    @Test
    fun maps_request_timeout_to_timeout_kind() = runTest {
        val result = service { throw HttpRequestTimeoutException("share-pdf", 30_000L) }.shareCart("t", request)

        assertEquals(NetworkErrorKind.Timeout, (result as NetworkResult.NetworkError).kind)
    }

    @Test
    fun maps_unknown_host_to_connectivity_kind() = runTest {
        val result = service { throw UnknownHostException("no dns") }.shareCart("t", request)

        assertEquals(NetworkErrorKind.Connectivity, (result as NetworkResult.NetworkError).kind)
    }
}
