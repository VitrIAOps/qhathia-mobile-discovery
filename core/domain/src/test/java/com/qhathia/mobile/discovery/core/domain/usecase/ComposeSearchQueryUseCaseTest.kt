package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.SearchFilterOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComposeSearchQueryUseCaseTest {

    private val composeSearchQuery = ComposeSearchQueryUseCase()

    @Test
    fun `given no selection then returns trimmed base query`() {
        val result = composeSearchQuery("  busco un taladro  ", emptyList(), null)

        assertEquals("busco un taladro", result)
    }

    @Test
    fun `given one filter then appends criterion and option lowercased`() {
        val result = composeSearchQuery(
            "busco un taladro",
            listOf(SearchFilterOption("Voltaje", "12V")),
            null,
        )

        assertEquals("busco un taladro voltaje 12v", result)
    }

    @Test
    fun `given multiple filters then appends all in order`() {
        val result = composeSearchQuery(
            "busco un taladro",
            listOf(SearchFilterOption("Marca", "Bosch"), SearchFilterOption("Voltaje", "12V")),
            null,
        )

        assertEquals("busco un taladro marca bosch voltaje 12v", result)
    }

    @Test
    fun `given suggestion only then appends suggestion lowercased`() {
        val result = composeSearchQuery("busco un taladro", emptyList(), "Para uso doméstico")

        assertEquals("busco un taladro para uso doméstico", result)
    }

    @Test
    fun `given filters and suggestion then appends filters then suggestion`() {
        val result = composeSearchQuery(
            "busco un taladro",
            listOf(SearchFilterOption("Marca", "Bosch")),
            "Inalámbrico",
        )

        assertEquals("busco un taladro marca bosch inalámbrico", result)
    }

    @Test
    fun `given empty base with a term then returns the term without leading space`() {
        val result = composeSearchQuery("", emptyList(), "Inalámbrico")

        assertEquals("inalámbrico", result)
    }

    @Test
    fun `given selection cleared then reproduces the base query`() {
        val base = "busco un taladro"
        // Selecting then de-selecting all returns to an empty selection, which yields the base.
        val refined = composeSearchQuery(base, listOf(SearchFilterOption("Voltaje", "12V")), "Percutor")
        val backToBase = composeSearchQuery(base, emptyList(), null)

        assertEquals("busco un taladro voltaje 12v percutor", refined)
        assertEquals(base, backToBase)
    }
}
