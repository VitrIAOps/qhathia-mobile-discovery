package com.qhathia.mobile.discovery.core.data.cart

import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartRepositoryImplTest {

    private fun repository() = CartRepositoryImpl(CartSessionLocalDataSource())

    private fun item(id: String, qty: Int = 1, max: Int = 99) = CartItem(
        productId = id,
        name = "Product $id",
        priceUnit = 10.0,
        imageUrl = "",
        availability = "AVAILABLE",
        quantity = qty,
        maxQuantity = max,
    )

    @Test
    fun `starts empty`() = runTest {
        assertEquals(Cart.EMPTY, repository().observe().first())
    }

    @Test
    fun `add merges the same product`() = runTest {
        val repository = repository()

        repository.addProduct(item("p1", qty = 1))
        repository.addProduct(item("p1", qty = 2))

        val cart = repository.observe().first()
        assertEquals(1, cart.items.size)
        assertEquals(3, cart.items.first().quantity)
    }

    @Test
    fun `setQuantity bounds to the stock max`() = runTest {
        val repository = repository()
        repository.addProduct(item("p1", qty = 1, max = 3))

        repository.setQuantity("p1", 9)

        assertEquals(3, repository.observe().first().items.first().quantity)
    }

    @Test
    fun `remove drops the line`() = runTest {
        val repository = repository()
        repository.addProduct(item("p1"))

        repository.removeProduct("p1")

        assertTrue(repository.observe().first().isEmpty)
    }

    @Test
    fun `observe emits updates then clear resets to empty`() = runTest {
        val repository = repository()

        repository.observe().test {
            assertEquals(Cart.EMPTY, awaitItem())

            repository.addProduct(item("p1"))
            assertEquals(1, awaitItem().items.size)

            repository.clear()
            assertEquals(Cart.EMPTY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh updates prices and caps in one step`() = runTest {
        val repository = repository()
        repository.addProduct(item("p1", qty = 6))

        val refresh = repository.refresh(
            mapOf("p1" to ProductSnapshot("p1", unitPrice = 8.0, availability = "AVAILABLE", stock = 3)),
        )

        val line = repository.observe().first().items.single()
        assertEquals(8.0, line.priceUnit)
        assertEquals(3, line.quantity)
        assertEquals(1, refresh.priceChanges.size)
        assertEquals(1, refresh.capped.size)
    }

    @Test
    fun `refresh keeps a line added meanwhile out of the removed list`() = runTest {
        val repository = repository()
        repository.addProduct(item("p1"))
        repository.addProduct(item("p2"))

        repository.refresh(mapOf("p1" to ProductSnapshot("p1", 10.0, "AVAILABLE", null)))

        assertEquals(listOf("p1"), repository.observe().first().items.map { it.productId })
    }

    @Test
    fun `update prices changes only the listed lines`() = runTest {
        val repository = repository()
        repository.addProduct(item("p1"))
        repository.addProduct(item("p2"))

        repository.updatePrices(listOf(CartPriceChange("p2", "Product p2", 10.0, 7.5)))

        assertEquals(listOf(10.0, 7.5), repository.observe().first().items.map { it.priceUnit })
    }
}
