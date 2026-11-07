package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.SearchFiltersDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationSearchApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(body: String = """{"understood":true}""") = ConversationSearchApiService(
        KtorClientFactory.create(
            MockEngine { req ->
                captured = req
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    private fun sentBody(): String = (captured!!.body as TextContent).text

    @Test
    fun filters_travel_next_to_the_bare_text() = runTest {
        service().search("t", "taladro", "es", filters = SearchFiltersDto(brands = listOf("Bosch")))

        val body = sentBody()
        assertTrue(body.contains("\"transcribedText\":\"taladro\""), body)
        assertTrue(body.contains("\"filters\":{\"brands\":[\"Bosch\"]}"), body)
    }

    @Test
    fun no_filters_key_without_chips() = runTest {
        service().search("t", "taladro", "es")

        assertFalse(sentBody().contains("filters"), sentBody())
    }

    @Test
    fun parses_values_and_alternatives_only() = runTest {
        val body = """
            {"understood":true,"alternativesOnly":true,"missingCategory":"ventilador",
             "filters":[{"criterion":"Tipo","options":["Inalámbrico"],"values":["inalambrico"]}]}
        """.trimIndent()

        val dto = (service(body).search("t", "ventilador", "es") as NetworkResult.Success).data

        assertTrue(dto.alternativesOnly)
        assertEquals("ventilador", dto.missingCategory)
        assertEquals(listOf("inalambrico"), dto.filters.single().values)
    }
}
