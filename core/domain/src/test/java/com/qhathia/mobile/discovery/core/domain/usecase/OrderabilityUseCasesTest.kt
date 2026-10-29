package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.ProductDetailFixtures
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

    /** Answers each product id from [answers]; ids not listed are AVAILABLE and active. */
    private class FakeDetails(private val answers: Map<String, Result<ProductDetail>>) : ProductDetailRepository {
        val requested = mutableListOf<String>()
        override suspend fun getProduct(id: String): Result<ProductDetail> {
            requested += id
            return answers[id] ?: Result.success(ProductDetailFixtures.productDetail(id = id))
        }
        override suspend fun getSuggestions(id: String): Result<List<Product>> = Result.success(emptyList())
    }

    private val basket = listOf("a", "b", "c", "d").map { CartFixtures.cartItem(productId = it) }

    @Test
    fun `recheck flags inactive, low stock and missing products`() = runTest {
        val details = FakeDetails(
            mapOf(
                "a" to Result.success(ProductDetailFixtures.productDetail(id = "a", active = false)),
                "b" to Result.success(ProductDetailFixtures.productDetail(id = "b", availability = "LOW_STOCK")),
                "c" to Result.failure(DetailException(DetailErrorCategory.NotFound)),
            ),
        )

        val flagged = RevalidateCartUseCase(details)(basket)

        assertEquals(setOf("a", "b", "c"), flagged)
        assertEquals(basket.map { it.productId }.toSet(), details.requested.toSet())
    }

    @Test
    fun `recheck with every product orderable flags nothing`() = runTest {
        assertEquals(emptySet<String>(), RevalidateCartUseCase(FakeDetails(emptyMap()))(basket))
    }

    @Test
    fun `recheck does not blame a product it could not read`() = runTest {
        val details = FakeDetails(mapOf("a" to Result.failure(DetailException(DetailErrorCategory.Network))))

        assertEquals(emptySet<String>(), RevalidateCartUseCase(details)(basket))
    }
}
