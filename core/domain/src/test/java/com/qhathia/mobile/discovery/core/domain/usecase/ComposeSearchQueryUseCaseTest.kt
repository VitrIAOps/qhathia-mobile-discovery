package com.qhathia.mobile.discovery.core.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComposeSearchQueryUseCaseTest {

    private val composeSearchQuery = ComposeSearchQueryUseCase()

    @Test
    fun `no suggestion returns the trimmed base`() {
        assertEquals("busco un taladro", composeSearchQuery("  busco un taladro ", null))
    }

    @Test
    fun `suggestion is appended lowercased`() {
        assertEquals(
            "busco un taladro para uso doméstico",
            composeSearchQuery("busco un taladro", "Para uso doméstico"),
        )
    }

    @Test
    fun `blank suggestion is ignored`() {
        assertEquals("taladro", composeSearchQuery("taladro", "   "))
    }

    @Test
    fun `blank base keeps only the suggestion`() {
        assertEquals("para uso doméstico", composeSearchQuery("", "para uso doméstico"))
    }
}
