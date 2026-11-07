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

    // --- refreshedWith / withPricesUpdated (order-price-check D2) ---

    private fun snapshot(id: String, price: Double, availability: String = "AVAILABLE", stock: Int? = null) =
        ProductSnapshot(id = id, unitPrice = price, availability = availability, stock = stock)

    @Test
    fun `refresh updates a changed price and reports it`() {
        val refresh = Cart(listOf(item("p1", 210.0, 1))).refreshedWith(mapOf("p1" to snapshot("p1", 199.0)))

        assertEquals(199.0, refresh.cart.items.single().priceUnit)
        assertEquals(listOf(CartPriceChange("p1", "Product p1", 210.0, 199.0)), refresh.priceChanges)
        assertTrue(refresh.hasChanges)
    }

    @Test
    fun `refresh ignores a difference below one cent`() {
        val refresh = Cart(listOf(item("p1", 80.0, 1))).refreshedWith(mapOf("p1" to snapshot("p1", 80.004)))

        assertTrue(refresh.priceChanges.isEmpty())
        assertFalse(refresh.hasChanges)
    }

    @Test
    fun `refresh removes absent, unavailable and sold-out lines`() {
        val cart = Cart(listOf(item("gone", 10.0, 1), item("low", 10.0, 1), item("zero", 10.0, 1), item("ok", 10.0, 1)))

        val refresh = cart.refreshedWith(
            mapOf(
                "low" to snapshot("low", 10.0, availability = "LOW_STOCK"),
                "zero" to snapshot("zero", 10.0, stock = 0),
                "ok" to snapshot("ok", 10.0),
            ),
        )

        assertEquals(listOf("ok"), refresh.cart.items.map { it.productId })
        assertEquals(listOf("Product gone", "Product low", "Product zero"), refresh.removedNames)
    }

    @Test
    fun `refresh caps the quantity to the stock`() {
        val refresh = Cart(listOf(item("p1", 10.0, 6))).refreshedWith(mapOf("p1" to snapshot("p1", 10.0, stock = 3)))

        val line = refresh.cart.items.single()
        assertEquals(3, line.quantity)
        assertEquals(3, line.maxQuantity)
        assertEquals(3, line.stock)
        assertEquals(listOf(CartQuantityCap("p1", "Product p1", 3)), refresh.capped)
    }

    @Test
    fun `refresh without stock keeps the availability cap`() {
        val refresh = Cart(listOf(item("p1", 10.0, 2))).refreshedWith(mapOf("p1" to snapshot("p1", 10.0)))

        assertEquals(99, refresh.cart.items.single().maxQuantity)
        assertTrue(refresh.capped.isEmpty())
    }

    @Test
    fun `prices updated only on the changed lines`() {
        val cart = Cart(listOf(item("p1", 210.0, 1), item("p2", 50.0, 1)))
            .withPricesUpdated(listOf(CartPriceChange("p1", "Product p1", 210.0, 199.0)))

        assertEquals(listOf(199.0, 50.0), cart.items.map { it.priceUnit })
    }
}
