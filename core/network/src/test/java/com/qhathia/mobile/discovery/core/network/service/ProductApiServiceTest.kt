package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KIOSK_SESSION_HEADER
import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProductApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(body: String) = ProductApiService(
        KtorClientFactory.create(
            MockEngine { req ->
                captured = req
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun list_products_hits_the_list_endpoint_and_parses_stock() = runTest {
        val body = """
            [{"id":"p1","name":"Sierra","price":210.0,"discountedPrice":199.0,"availability":"AVAILABLE","stock":3},
             {"id":"p2","name":"Broca","price":10.0,"availability":"CONSULT","stock":null}]
        """.trimIndent()

        val result = service(body).listProducts("device-jwt")

        val sent = captured!!
        assertEquals(HttpMethod.Get, sent.method)
        assertEquals("/api/v1/products", sent.url.encodedPath)
        assertEquals("Bearer device-jwt", sent.headers[HttpHeaders.Authorization])
        val products = (result as NetworkResult.Success).data
        assertEquals(3, products[0].stock)
        assertEquals(199.0, products[0].discountedPrice)
        assertNull(products[1].stock)
    }

    @Test
    fun detail_parses_stock() = runTest {
        val result = service("""{"id":"p1","name":"Sierra","price":210.0,"availability":"AVAILABLE","stock":0}""")
            .getProduct("t", "p1")

        assertEquals(0, (result as NetworkResult.Success).data.stock)
    }

    // --- add-customer-session-id (F-22) ---

    @Test
    fun detail_sends_the_customer_session_header() = runTest {
        service("""{"id":"p1","name":"Sierra","price":210.0}""").getProduct("t", "p1", kioskSession = "s-1")

        assertEquals("s-1", captured?.headers?.get(KIOSK_SESSION_HEADER))
    }

    @Test
    fun suggestions_and_list_do_not_send_the_customer_session_header() = runTest {
        service("""{"suggestions":[]}""").getSuggestions("t", "p1")
        assertNull(captured?.headers?.get(KIOSK_SESSION_HEADER))

        service("[]").listProducts("t")
        assertNull(captured?.headers?.get(KIOSK_SESSION_HEADER))
    }
}
