package com.qhathia.mobile.discovery.core.ui.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DiscountPercentTest {

    @Test
    fun given_twenty_percent_off_then_twenty() {
        assertEquals(20, discountPercent(199.0, 159.2))
    }

    @Test
    fun given_fraction_then_rounded() {
        assertEquals(33, discountPercent(30.0, 20.0))
    }

    @Test
    fun given_no_real_discount_then_null() {
        assertNull(discountPercent(100.0, null))
        assertNull(discountPercent(100.0, 100.0))
        assertNull(discountPercent(0.0, 0.0))
    }
}
