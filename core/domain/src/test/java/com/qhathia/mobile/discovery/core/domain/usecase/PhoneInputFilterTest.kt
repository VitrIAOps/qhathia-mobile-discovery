package com.qhathia.mobile.discovery.core.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhoneInputFilterTest {

    @Test
    fun letters_and_symbols_are_dropped() {
        assertEquals("987", filterPhoneInput("98a7b"))
        assertEquals("987654321", filterPhoneInput("(987)-654.321N"))
    }

    @Test
    fun prefix_and_spaces_are_kept() {
        assertEquals("+51 987 654 321", filterPhoneInput("+51 987 654 321"))
        assertEquals("987 654 321", filterPhoneInput("987 654 321"))
    }

    @Test
    fun plus_only_at_the_start() {
        assertEquals("+51987", filterPhoneInput("+51+987"))
        assertEquals("51987", filterPhoneInput("51+987"))
    }

    @Test
    fun repeated_and_leading_spaces_are_collapsed() {
        assertEquals("987 654", filterPhoneInput("  987   654"))
    }

    @Test
    fun digits_are_capped() {
        assertEquals("51987654321", filterPhoneInput("519876543219999"))
    }

    @Test
    fun non_ascii_digits_are_dropped() {
        assertEquals("98", filterPhoneInput("9٣8"))
    }
}
