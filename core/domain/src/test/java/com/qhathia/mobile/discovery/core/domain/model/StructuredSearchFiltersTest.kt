package com.qhathia.mobile.discovery.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StructuredSearchFiltersTest {

    private val panel = listOf(
        SearchFilter("Marca", listOf("Bosch", "DeWalt")),
        SearchFilter("Tipo", listOf("Inalámbrico", "Con cable"), values = listOf("inalambrico", "con-cable")),
    )

    @Test
    fun `brand option goes to brands as its label`() {
        val filters = StructuredSearchFilters.from(listOf(SearchFilterOption("Marca", "Bosch")), panel)

        assertEquals(StructuredSearchFilters(brands = listOf("Bosch")), filters)
    }

    @Test
    fun `tag option goes to tags as its value`() {
        val filters = StructuredSearchFilters.from(listOf(SearchFilterOption("Tipo", "Inalámbrico")), panel)

        assertEquals(StructuredSearchFilters(tags = listOf("inalambrico")), filters)
    }

    @Test
    fun `brand criterion matched ignoring case`() {
        val lower = listOf(SearchFilter("marca", listOf("Bosch")))

        val filters = StructuredSearchFilters.from(listOf(SearchFilterOption("marca", "Bosch")), lower)

        assertEquals(listOf("Bosch"), filters.brands)
    }

    @Test
    fun `option missing from the panel is dropped`() {
        val filters = StructuredSearchFilters.from(
            listOf(SearchFilterOption("Voltaje", "18V"), SearchFilterOption("Marca", "Makita")),
            panel,
        )

        assertTrue(filters.isEmpty)
    }

    @Test
    fun `brands and tags together`() {
        val filters = StructuredSearchFilters.from(
            listOf(SearchFilterOption("Marca", "Bosch"), SearchFilterOption("Tipo", "Inalámbrico")),
            panel,
        )

        assertEquals(StructuredSearchFilters(listOf("Bosch"), listOf("inalambrico")), filters)
    }

    @Test
    fun `values default to the options`() {
        assertEquals(listOf("Bosch", "DeWalt"), panel.first().values)
    }
}
