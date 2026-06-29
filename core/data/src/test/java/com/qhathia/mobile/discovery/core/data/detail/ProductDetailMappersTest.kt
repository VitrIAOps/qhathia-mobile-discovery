package com.qhathia.mobile.discovery.core.data.detail

import com.qhathia.mobile.discovery.core.network.dto.SuggestedProductDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductDetailMappersTest {

    @Test
    fun given_suggested_product_when_mapped_then_carries_rendered_fields() {
        val dto = SuggestedProductDto(
            id = "s1",
            categoryId = "c1",
            categoryName = "Accesorios",
            name = "Set de brocas",
            price = 49.9,
            availability = "AVAILABLE",
            allowDiscountInquiry = true,
            primaryImageUrl = "s1.jpg",
            brand = "Bosch",
        )

        val product = dto.toDomain()

        assertEquals("s1", product.id)
        assertEquals("Set de brocas", product.name)
        assertEquals("", product.description)
        assertEquals(49.9, product.price)
        assertEquals("AVAILABLE", product.availability)
        assertEquals("Accesorios", product.categoryName)
        assertEquals("s1.jpg", product.imageUrl)
        assertEquals(true, product.allowDiscountInquiry)
    }

    @Test
    fun given_null_image_when_mapped_then_image_url_is_empty() {
        val product = SuggestedProductDto(id = "s2", name = "Guantes", primaryImageUrl = null).toDomain()

        assertEquals("", product.imageUrl)
    }
}
