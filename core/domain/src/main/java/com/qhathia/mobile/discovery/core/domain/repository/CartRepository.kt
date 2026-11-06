package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.CartRefresh
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Port for the session-scoped cart basket. Implemented in `core:data` as an in-memory singleton (the
 * basket is ephemeral session state, cleared whenever the customer session ends — see
 * [com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase]).
 */
interface CartRepository {

    /** Emits the current [Cart] immediately and again whenever the basket changes. */
    fun observe(): Flow<Cart>

    /** Adds [item] to the basket, merging into an existing line and bounding by stock. */
    suspend fun addProduct(item: CartItem)

    /** Sets the line for [productId] to [quantity] (bounded by stock; below 1 removes the line). */
    suspend fun setQuantity(productId: String, quantity: Int)

    /** Removes the line for [productId] from the basket. */
    suspend fun removeProduct(productId: String)

    /** Empties the basket. */
    suspend fun clear()

    /**
     * Refreshes the basket against [snapshots] in one atomic step ([Cart.refreshedWith]), so a change
     * made meanwhile is never lost. Does not end the customer session.
     */
    suspend fun refresh(snapshots: Map<String, ProductSnapshot>): CartRefresh

    /** Sets each changed line to its new unit price in one atomic step. Does not end the customer session. */
    suspend fun updatePrices(changes: List<CartPriceChange>)
}
