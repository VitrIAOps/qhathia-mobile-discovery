package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [CartRepository] for tests. Applies the same [Cart] transforms as the production
 * implementation (merge + stock bound + remove-on-zero), so use-case/ViewModel tests exercise the
 * real business rules.
 */
class FakeCartRepository(
    initial: Cart = Cart.EMPTY,
) : CartRepository {

    private val state = MutableStateFlow(initial)

    /** Current snapshot, for convenient assertions. */
    val current: Cart get() = state.value

    override fun observe(): Flow<Cart> = state.asStateFlow()

    override suspend fun addProduct(item: CartItem) {
        state.value = state.value.withProductAdded(item)
    }

    override suspend fun setQuantity(productId: String, quantity: Int) {
        state.value = state.value.withQuantitySet(productId, quantity)
    }

    override suspend fun removeProduct(productId: String) {
        state.value = state.value.withProductRemoved(productId)
    }

    override suspend fun clear() {
        state.value = Cart.EMPTY
    }
}
