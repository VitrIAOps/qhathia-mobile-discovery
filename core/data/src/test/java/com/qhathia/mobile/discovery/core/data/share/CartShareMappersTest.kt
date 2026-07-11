package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.network.dto.ShareCartResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CartShareMappersTest {

    private fun item(id: String = "p1", qty: Int = 1) = CartItem(
        productId = id,
        name = "Taladro",
        priceUnit = 189.0,
        imageUrl = "t.jpg",
        availability = "AVAILABLE",
        quantity = qty,
        maxQuantity = 99,
    )

    @Test
    fun given_name_and_items_when_building_request_then_trims_and_normalizes() {
        val request = buildShareCartRequest(
            name = "  Ana Pérez  ",
            phone = "+51 987 654 321",
            items = listOf(item("p1", 2), item("p2", 1)),
        )

        assertEquals("Ana Pérez", request.customerName)
        assertEquals("987654321", request.customerPhone)
        assertEquals(2, request.items.size)
        assertEquals("p1", request.items.first().productId)
        assertEquals(2, request.items.first().quantity)
    }

    @Test
    fun given_51_prefixed_number_when_building_request_then_prefix_stripped() {
        val request = buildShareCartRequest("Ana", "51987654321", listOf(item()))
        assertEquals("987654321", request.customerPhone)
    }

    @Test
    fun given_bare_number_when_building_request_then_unchanged() {
        val request = buildShareCartRequest("Ana", "987654321", listOf(item()))
        assertEquals("987654321", request.customerPhone)
    }

    @Test
    fun given_response_when_mapped_then_domain_share_fields_carried() {
        val dto = ShareCartResponseDto(
            status = "SENT",
            messageId = "wamid.1",
            pdfUrl = "https://r2.dev/cotizacion.pdf",
            error = null,
        )

        val share = dto.toDomain()

        assertEquals("SENT", share.status)
        assertEquals("https://r2.dev/cotizacion.pdf", share.pdfUrl)
        assertEquals("wamid.1", share.messageId)
    }

    @Test
    fun given_failed_response_without_message_when_mapped_then_null_message_id() {
        val dto = ShareCartResponseDto(status = "FAILED", messageId = null, pdfUrl = "https://r2.dev/c.pdf")

        val share = dto.toDomain()

        assertEquals("FAILED", share.status)
        assertNull(share.messageId)
    }
}
