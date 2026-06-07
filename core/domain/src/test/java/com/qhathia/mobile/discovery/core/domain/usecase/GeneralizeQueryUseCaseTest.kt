package com.qhathia.mobile.discovery.core.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeneralizeQueryUseCaseTest {

    private val useCase = GeneralizeQueryUseCase()

    @Test
    fun given_noun_with_spec_qualifier_then_drops_qualifier_and_pluralizes() {
        assertEquals("Taladros", useCase("Taladro 20v"))
    }

    @Test
    fun given_singular_noun_ending_in_vowel_then_adds_s() {
        assertEquals("Lamparas", useCase("lampara"))
    }

    @Test
    fun given_noun_ending_in_consonant_then_adds_es() {
        assertEquals("Tijerales", useCase("tijeral"))
    }

    @Test
    fun given_already_plural_then_unchanged_word_capitalized() {
        assertEquals("Taladros", useCase("taladros"))
    }

    @Test
    fun given_multi_word_query_then_uses_head_noun() {
        assertEquals("Lamparas", useCase("lampara de pared vintage"))
    }

    @Test
    fun given_blank_query_then_returns_trimmed_input() {
        assertEquals("", useCase("   "))
    }
}
