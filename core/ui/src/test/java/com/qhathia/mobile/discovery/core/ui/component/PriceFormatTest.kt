package com.qhathia.mobile.discovery.core.ui.component

import com.qhathia.mobile.discovery.core.ui.format.formatPrice
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class PriceFormatTest {

    private val original = Locale.getDefault()

    @BeforeEach
    fun spanishSpain() = Locale.setDefault(Locale.forLanguageTag("es-ES"))

    @AfterEach
    fun restore() = Locale.setDefault(original)

    @Test
    fun formatPrice_givenThousands_usesCommaGrouping() {
        assertEquals("S/ 1,250.00", formatPrice(1250.0))
        assertEquals("S/ 1,101.90", formatPrice(1101.9))
    }

    @Test
    fun formatPrice_givenCents_keepsTwoDecimals() {
        assertEquals("S/ 0.50", formatPrice(0.5))
        assertEquals("S/ 380.00", formatPrice(380.0))
    }
}
