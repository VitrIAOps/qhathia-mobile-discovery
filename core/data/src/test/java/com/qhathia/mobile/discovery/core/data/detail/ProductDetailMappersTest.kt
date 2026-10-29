package com.qhathia.mobile.discovery.core.data.detail

import com.qhathia.mobile.discovery.core.network.dto.ProductDetailResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SuggestedProductDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
            discountedPrice = 39.9,
            discountEndsAt = "2026-10-25T05:00:00Z",
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
        assertEquals(39.9, product.discountedPrice)
        assertEquals("2026-10-25T05:00:00Z", product.discountEndsAt)
    }

    @Test
    fun given_detail_with_discount_when_mapped_then_carries_discounted_price() {
        val detail = ProductDetailResponseDto(
            id = "p1",
            price = 35.9,
            discountedPrice = 30.9,
            discountEndsAt = "2026-10-25T05:00:00Z",
        ).toDomain()

        assertEquals(35.9, detail.price)
        assertEquals(30.9, detail.discountedPrice)
        assertEquals("2026-10-25T05:00:00Z", detail.discountEndsAt)
    }

    @Test
    fun given_detail_without_discount_when_mapped_then_discounted_price_is_null() {
        val detail = ProductDetailResponseDto(id = "p1", price = 35.9).toDomain()

        assertNull(detail.discountedPrice)
        assertNull(detail.discountEndsAt)
    }

    @Test
    fun given_null_image_when_mapped_then_image_url_is_empty() {
        val product = SuggestedProductDto(id = "s2", name = "Guantes", primaryImageUrl = null).toDomain()

        assertEquals("", product.imageUrl)
    }

    @Test
    fun given_detail_active_flag_when_mapped_then_carried_as_is() {
        assertEquals(false, ProductDetailResponseDto(id = "p1", active = false).toDomain().active)
        assertEquals(true, ProductDetailResponseDto(id = "p1", active = true).toDomain().active)
        assertEquals(null, ProductDetailResponseDto(id = "p1").toDomain().active) // absent: unknown
    }
}
