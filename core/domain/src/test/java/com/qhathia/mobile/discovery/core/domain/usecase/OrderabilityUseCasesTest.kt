package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartPricingRepository
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrderabilityUseCasesTest {

    private val isOrderable = IsOrderableUseCase()

    @Test
    fun `only active AVAILABLE products are orderable`() {
        assertTrue(isOrderable("AVAILABLE", active = true))
        assertTrue(isOrderable("AVAILABLE", active = null)) // list endpoints do not send `active`
        assertTrue(isOrderable("available"))
        assertFalse(isOrderable("AVAILABLE", active = false))
        assertFalse(isOrderable("LOW_STOCK"))
        assertFalse(isOrderable("OUT_OF_STOCK"))
        assertFalse(isOrderable("CONSULT"))
        assertFalse(isOrderable(""))
    }

    @Test
    fun `add to cart ignores a product that is not orderable`() = runTest {
        val cart = FakeCartRepository()
        val add = AddToCartUseCase(cart)

        add(CartFixtures.cartItem(productId = "low", availability = "LOW_STOCK"))
        add(CartFixtures.cartItem(productId = "ok", availability = "AVAILABLE"))

        assertEquals(listOf("ok"), cart.current.items.map { it.productId })
    }

    private val pricing = FakeCartPricingRepository()

    private val basket = listOf("a", "b", "c", "d").map { CartFixtures.cartItem(productId = it) }

    private fun available(id: String, stock: Int? = null) =
        ProductSnapshot(id = id, unitPrice = 10.0, availability = "AVAILABLE", stock = stock)

    @Test
    fun `recheck flags missing, not available and sold-out products with one request`() = runTest {
        pricing.answer(
            ProductSnapshot("b", 10.0, "LOW_STOCK", null),
            available("c", stock = 0),
            available("d"),
        )

        val flagged = RevalidateCartUseCase(pricing)(basket)

        assertEquals(setOf("a", "b", "c"), flagged)
        assertEquals(1, pricing.calls)
    }

    @Test
    fun `recheck with every product orderable flags nothing`() = runTest {
        pricing.answer(available("a"), available("b"), available("c"), available("d", stock = 4))

        assertEquals(emptySet<String>(), RevalidateCartUseCase(pricing)(basket))
    }

    @Test
    fun `recheck blames nothing when the products cannot be read`() = runTest {
        pricing.result = Result.failure(IllegalStateException("offline"))

        assertEquals(emptySet<String>(), RevalidateCartUseCase(pricing)(basket))
    }

    @Test
    fun `stock zero is not orderable and a null stock is`() {
        assertFalse(isOrderable("AVAILABLE", stock = 0))
        assertTrue(isOrderable("AVAILABLE", stock = 1))
        assertTrue(isOrderable("AVAILABLE", stock = null))
    }

    @Test
    fun `add to cart refuses a product with no stock left`() = runTest {
        val cart = FakeCartRepository()

        val result = AddToCartUseCase(cart)(CartFixtures.cartItem(productId = "zero", stock = 0))

        assertEquals(AddToCartResult.NotOrderable, result)
        assertTrue(cart.current.isEmpty)
    }

    @Test
    fun `cap follows the stock when known`() {
        assertEquals(3, CartConstants.maxQuantityFor("AVAILABLE", 3))
        assertEquals(99, CartConstants.maxQuantityFor("AVAILABLE", 500))
        assertEquals(1, CartConstants.maxQuantityFor("AVAILABLE", 0))
        assertEquals(99, CartConstants.maxQuantityFor("AVAILABLE", null))
        assertEquals(1, CartConstants.maxQuantityFor("CONSULT", null))
    }

    @Test
    fun `refresh use case leaves the basket untouched on failure`() = runTest {
        val cart = FakeCartRepository(CartFixtures.cart())
        val before = cart.current
        pricing.result = Result.failure(IllegalStateException("offline"))

        val result = RefreshCartUseCase(pricing, cart)()

        assertTrue(result.isFailure)
        assertEquals(before, cart.current)
    }

    @Test
    fun `refresh use case applies the snapshots`() = runTest {
        val cart = FakeCartRepository(Cart(listOf(CartFixtures.cartItem(productId = "p1", priceUnit = 210.0))))
        pricing.answer(ProductSnapshot("p1", 199.0, "AVAILABLE", 5))

        val refresh = RefreshCartUseCase(pricing, cart)().getOrThrow()

        assertEquals(199.0, cart.current.items.single().priceUnit)
        assertEquals(1, refresh.priceChanges.size)
    }

    @Test
    fun `update prices use case applies the changes`() = runTest {
        val cart = FakeCartRepository(Cart(listOf(CartFixtures.cartItem(productId = "p1", priceUnit = 210.0))))

        UpdateCartPricesUseCase(cart)(listOf(CartPriceChange("p1", "Sierra", 210.0, 199.0)))

        assertEquals(199.0, cart.current.items.single().priceUnit)
    }
}
