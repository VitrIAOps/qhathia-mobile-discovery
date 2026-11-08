package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.network.dto.OrderItemResponseDto
import com.qhathia.mobile.discovery.core.network.dto.OrderResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderMappersTest {

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
    fun given_bare_number_when_normalized_then_unchanged() {
        assertEquals("987654321", "987654321".toApiMobileNumber())
    }

    @Test
    fun given_spaced_number_when_normalized_then_whitespace_stripped() {
        assertEquals("987654321", "987 654 321".toApiMobileNumber())
    }

    @Test
    fun given_plus51_prefixed_when_normalized_then_prefix_stripped() {
        assertEquals("987654321", "+51 987 654 321".toApiMobileNumber())
    }

    @Test
    fun given_51_prefixed_when_normalized_then_prefix_stripped() {
        assertEquals("987654321", "51987654321".toApiMobileNumber())
    }

    @Test
    fun given_plus51_no_space_when_normalized_then_prefix_stripped() {
        assertEquals("987654321", "+51987654321".toApiMobileNumber())
    }

    @Test
    fun given_name_and_items_when_building_request_then_trims_and_maps_lines() {
        val request = buildCreateOrderRequest(
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
    fun given_response_when_mapped_then_domain_order_fields_carried() {
        val dto = OrderResponseDto(
            id = "o1",
            status = "PENDING",
            subtotal = 250.0,
            total = 250.0,
            customerName = "Ana",
            customerPhone = "987654321",
            items = listOf(
                OrderItemResponseDto(
                    id = "i1",
                    productId = "p1",
                    productName = "Taladro",
                    quantity = 2,
                    unitPriceSnapshot = 100.0,
                    lineTotal = 200.0,
                ),
            ),
        )

        val order = dto.toDomain()

        assertEquals("o1", order.id)
        assertEquals("PENDING", order.status)
        assertEquals(250.0, order.total)
        assertEquals(1, order.items.size)
        assertEquals("Taladro", order.items.first().productName)
        assertEquals(200.0, order.items.first().lineTotal)
    }

    // --- add-customer-session-id (F-22) ---

    @Test
    fun given_session_id_when_building_request_then_it_is_in_the_body() {
        val request = buildCreateOrderRequest("Ana", "987654321", listOf(item()), sessionId = "s-1")

        assertEquals("s-1", request.sessionId)
    }

    @Test
    fun given_no_session_id_when_building_request_then_it_is_omitted() {
        assertEquals(null, buildCreateOrderRequest("Ana", "987654321", listOf(item())).sessionId)
    }
}
