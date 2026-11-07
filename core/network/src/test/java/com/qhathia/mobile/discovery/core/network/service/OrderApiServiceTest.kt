package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.CreateOrderRequestDto
import com.qhathia.mobile.discovery.core.network.dto.OrderItemRequestDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrderApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(handler: MockRequestHandleScope.() -> HttpResponseData) = OrderApiService(
        KtorClientFactory.create(
            MockEngine { req ->
                captured = req
                handler()
            },
        ),
    )

    private fun request(expected: Double?) = CreateOrderRequestDto(
        customerName = "Ana",
        customerPhone = "987654321",
        items = listOf(OrderItemRequestDto(productId = "p1", quantity = 1, expectedUnitPrice = expected)),
    )

    private fun sentBody(): String = (captured!!.body as TextContent).text

    private val created: MockRequestHandleScope.() -> HttpResponseData = {
        respond("""{"id":"o1","total":210.0}""", HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun sends_the_expected_unit_price() = runTest {
        service(created).createOrder("t", request(210.0))

        assertTrue(sentBody().contains("\"expectedUnitPrice\":210.0"), sentBody())
    }

    @Test
    fun omits_the_expected_unit_price_when_null() = runTest {
        service(created).createOrder("t", request(null))

        assertFalse(sentBody().contains("expectedUnitPrice"), sentBody())
    }

    @Test
    fun price_changed_keeps_the_changes() = runTest {
        val body = """{"status":409,"errorCode":"PRICE_CHANGED","detail":"cambió",
            "changes":[{"productId":"p1","productName":"QA X OK","expectedUnitPrice":210.0,"currentUnitPrice":199.0}]}"""

        val result = service {
            respond(body, HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/problem+json"))
        }.createOrder("t", request(210.0))

        val error = result as NetworkResult.HttpError
        assertEquals("PRICE_CHANGED", error.errorCode)
        assertEquals(199.0, error.problem!!.changes.single().currentUnitPrice)
    }
}
