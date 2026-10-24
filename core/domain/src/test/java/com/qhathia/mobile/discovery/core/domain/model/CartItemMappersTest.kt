package com.qhathia.mobile.discovery.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CartItemMappersTest {

    private fun product(discountedPrice: Double? = null) = Product(
        id = "p1",
        name = "Foco LED",
        description = "",
        price = 35.90,
        availability = "AVAILABLE",
        categoryName = "Iluminación",
        imageUrl = "",
        discountedPrice = discountedPrice,
    )

    private fun detail(discountedPrice: Double? = null) = ProductDetail(
        id = "p1",
        name = "Foco LED",
        description = "",
        price = 35.90,
        availability = "AVAILABLE",
        categoryName = "Iluminación",
        images = emptyList(),
        discountedPrice = discountedPrice,
    )

    @Test
    fun `product with active discount uses discounted price`() {
        assertEquals(30.90, product(discountedPrice = 30.90).toCartItem().priceUnit)
    }

    @Test
    fun `product without discount uses list price`() {
        assertEquals(35.90, product().toCartItem().priceUnit)
    }

    @Test
    fun `detail with active discount uses discounted price`() {
        assertEquals(30.90, detail(discountedPrice = 30.90).toCartItem(quantity = 2).priceUnit)
    }

    @Test
    fun `detail without discount uses list price`() {
        assertEquals(35.90, detail().toCartItem().priceUnit)
    }
}
