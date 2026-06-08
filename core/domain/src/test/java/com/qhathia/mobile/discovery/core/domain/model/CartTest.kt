package com.qhathia.mobile.discovery.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CartTest {

    private fun item(id: String, price: Double, qty: Int, max: Int = 99) = CartItem(
        productId = id,
        name = "Product $id",
        priceUnit = price,
        imageUrl = "",
        availability = "AVAILABLE",
        quantity = qty,
        maxQuantity = max,
    )

    @Test
    fun `totals sum quantities and subtotals`() {
        val cart = Cart(listOf(item("p1", 189.0, 1), item("p2", 50.0, 2)))

        assertEquals(3, cart.totalItemCount)
        assertEquals(289.0, cart.totalAmount)
        assertEquals(100.0, cart.items[1].lineSubtotal)
        assertFalse(cart.isEmpty)
    }

    @Test
    fun `EMPTY cart is empty with zero totals`() {
        assertTrue(Cart.EMPTY.isEmpty)
        assertEquals(0, Cart.EMPTY.totalItemCount)
        assertEquals(0.0, Cart.EMPTY.totalAmount)
    }

    @Test
    fun `adding a new product appends a line`() {
        val cart = Cart.EMPTY.withProductAdded(item("p1", 10.0, 1))

        assertEquals(1, cart.items.size)
        assertEquals(1, cart.items.first().quantity)
    }

    @Test
    fun `adding an existing product merges quantities`() {
        val cart = Cart(listOf(item("p1", 10.0, 1)))
            .withProductAdded(item("p1", 10.0, 2))

        assertEquals(1, cart.items.size)
        assertEquals(3, cart.items.first().quantity)
    }

    @Test
    fun `adding is capped at the stock max`() {
        val cart = Cart(listOf(item("p1", 10.0, 4, max = 5)))
            .withProductAdded(item("p1", 10.0, 10, max = 5))

        assertEquals(5, cart.items.first().quantity)
    }

    @Test
    fun `no limit on distinct products`() {
        var cart = Cart.EMPTY
        repeat(50) { cart = cart.withProductAdded(item("p$it", 1.0, 1)) }

        assertEquals(50, cart.items.size)
    }

    @Test
    fun `setting quantity clamps to the stock max`() {
        val cart = Cart(listOf(item("p1", 10.0, 1, max = 3)))
            .withQuantitySet("p1", 9)

        assertEquals(3, cart.items.first().quantity)
    }

    @Test
    fun `setting quantity below one removes the line`() {
        val cart = Cart(listOf(item("p1", 10.0, 1), item("p2", 5.0, 1)))
            .withQuantitySet("p1", 0)

        assertEquals(listOf("p2"), cart.items.map { it.productId })
    }

    @Test
    fun `removing a product drops its line`() {
        val cart = Cart(listOf(item("p1", 10.0, 1), item("p2", 5.0, 1)))
            .withProductRemoved("p1")

        assertEquals(listOf("p2"), cart.items.map { it.productId })
    }
}
