package com.qhathia.mobile.discovery.core.domain.usecase

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartUseCasesTest {

    private val repository = FakeCartRepository()

    @Test
    fun `add then observe emits the merged, bounded cart`() = runTest {
        val add = AddToCartUseCase(repository)

        add(CartFixtures.cartItem(productId = "p1", quantity = 1))
        add(CartFixtures.cartItem(productId = "p1", quantity = 2))

        val cart = ObserveCartUseCase(repository)().first()
        assertEquals(1, cart.items.size)
        assertEquals(3, cart.totalItemCount)
    }

    @Test
    fun `update quantity sets the line within bounds`() = runTest {
        repository.addProduct(CartFixtures.cartItem(productId = "p1", quantity = 1, maxQuantity = 5))

        UpdateCartItemQuantityUseCase(repository)("p1", 4)

        assertEquals(4, repository.current.items.first().quantity)
    }

    @Test
    fun `update quantity below one removes the line`() = runTest {
        repository.addProduct(CartFixtures.cartItem(productId = "p1", quantity = 1))

        UpdateCartItemQuantityUseCase(repository)("p1", 0)

        assertTrue(repository.current.isEmpty)
    }

    @Test
    fun `remove drops the line`() = runTest {
        repository.addProduct(CartFixtures.cartItem(productId = "p1"))
        repository.addProduct(CartFixtures.cartItem(productId = "p2"))

        RemoveCartItemUseCase(repository)("p1")

        assertEquals(listOf("p2"), repository.current.items.map { it.productId })
    }

    @Test
    fun `clear empties the cart`() = runTest {
        repository.addProduct(CartFixtures.cartItem(productId = "p1"))

        ObserveCartUseCase(repository)().test {
            assertEquals(1, awaitItem().items.size)
            ClearCartUseCase(repository)()
            assertTrue(awaitItem().isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
