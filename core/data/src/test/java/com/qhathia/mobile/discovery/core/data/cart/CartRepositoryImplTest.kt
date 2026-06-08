package com.qhathia.mobile.discovery.core.data.cart

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
}
