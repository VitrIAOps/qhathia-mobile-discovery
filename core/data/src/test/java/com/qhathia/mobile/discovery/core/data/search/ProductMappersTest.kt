package com.qhathia.mobile.discovery.core.data.search

import com.qhathia.mobile.discovery.core.network.dto.ConversationSearchResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SearchFilterDto
import com.qhathia.mobile.discovery.core.network.dto.SearchProductDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductMappersTest {

    @Test
    fun `given result with discount then maps discounted price`() {
        val product = SearchProductDto(
            id = "p1",
            price = 380.0,
            discountedPrice = 320.0,
            discountEndsAt = "2026-10-25T05:00:00Z",
        ).toDomain()

        assertEquals(320.0, product.discountedPrice)
        assertEquals("2026-10-25T05:00:00Z", product.discountEndsAt)
    }

    @Test
    fun `given result without discount then discounted price is null`() {
        val product = SearchProductDto(id = "p1", price = 380.0).toDomain()

        assertNull(product.discountedPrice)
    }

    @Test
    fun `given filters and suggestions then maps them to domain`() {
        val dto = ConversationSearchResponseDto(
            understood = true,
            results = listOf(SearchProductDto(id = "p1", name = "Taladro", price = 380.0)),
            displayText = "Encontré 1",
            filters = listOf(
                SearchFilterDto(criterion = "Marca", options = listOf("Bosch", "Makita")),
                SearchFilterDto(criterion = "Voltaje", options = listOf("12V", "18V", "20V")),
            ),
            suggestions = listOf("Para uso en casa", "Percutor"),
        )

        val domain = dto.toDomain()

        assertEquals(2, domain.filters.size)
        assertEquals("Marca", domain.filters.first().criterion)
        assertEquals(listOf("Bosch", "Makita"), domain.filters.first().options)
        assertEquals(listOf("Para uso en casa", "Percutor"), domain.suggestions)
    }

    @Test
    fun `given unhappy path with empty fields then filters and suggestions are empty`() {
        val dto = ConversationSearchResponseDto(
            understood = true,
            results = emptyList(),
            noResults = true,
            displayText = "Sin resultados",
        )

        val domain = dto.toDomain()

        assertTrue(domain.filters.isEmpty())
        assertTrue(domain.suggestions.isEmpty())
    }

    @Test
    fun `given suggestedFromCategory then maps it through`() {
        val dto = ConversationSearchResponseDto(
            understood = true,
            results = listOf(SearchProductDto(id = "p1", name = "Lámpara LED", price = 89.0)),
            noResults = true,
            suggestedFromCategory = "lámpara",
            displayText = "Sin resultados",
        )

        assertEquals("lámpara", dto.toDomain().suggestedFromCategory)
    }

    @Test
    fun `given no suggestedFromCategory then maps to null`() {
        val dto = ConversationSearchResponseDto(
            understood = true,
            results = listOf(SearchProductDto(id = "p1", name = "Taladro", price = 380.0)),
        )

        assertNull(dto.toDomain().suggestedFromCategory)
    }

    @Test
    fun `given result with stock then maps it`() {
        assertEquals(3, SearchProductDto(id = "p1", stock = 3).toDomain().stock)
        assertEquals(null, SearchProductDto(id = "p2").toDomain().stock)
    }

    @Test
    fun `given filter without values then values default to the options`() {
        assertEquals(listOf("Bosch"), SearchFilterDto("Marca", listOf("Bosch")).toDomain().values)
        assertEquals(listOf("inalambrico"), SearchFilterDto("Tipo", listOf("Inalámbrico"), listOf("inalambrico")).toDomain().values)
    }

    @Test
    fun `given alternatives only then maps the missing category`() {
        val domain = ConversationSearchResponseDto(
            results = listOf(SearchProductDto(id = "p1")),
            alternativesOnly = true,
            missingCategory = "ventilador",
        ).toDomain()

        assertTrue(domain.alternativesOnly)
        assertEquals("ventilador", domain.missingCategory)
    }
}
