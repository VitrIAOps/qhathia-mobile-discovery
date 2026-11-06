package com.qhathia.mobile.discovery.core.data.cart

import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.CartRefresh
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory holder for the session [Cart]. Unlike the DataStore-backed store session, the cart is
 * ephemeral session state (cleared whenever the customer session ends), so it lives only in a
 * process-scoped [MutableStateFlow]. The business rules are applied via the framework-free [Cart]
 * transforms so this layer and the test fake never drift.
 */
@Singleton
class CartSessionLocalDataSource @Inject constructor() {

    private val state = MutableStateFlow(Cart.EMPTY)

    fun observe(): Flow<Cart> = state.asStateFlow()

    fun addProduct(item: CartItem) = state.update { it.withProductAdded(item) }

    fun setQuantity(productId: String, quantity: Int) =
        state.update { it.withQuantitySet(productId, quantity) }

    fun removeProduct(productId: String) = state.update { it.withProductRemoved(productId) }

    fun clear() = state.update { Cart.EMPTY }

    /** Refreshes the basket against [snapshots] in one atomic update and returns what changed. */
    fun refresh(snapshots: Map<String, ProductSnapshot>): CartRefresh {
        var result: CartRefresh? = null
        state.update { cart -> cart.refreshedWith(snapshots).also { result = it }.cart }
        return checkNotNull(result)
    }

    /** Sets each changed line to its new unit price in one atomic update. */
    fun updatePrices(changes: List<CartPriceChange>) = state.update { it.withPricesUpdated(changes) }
}
