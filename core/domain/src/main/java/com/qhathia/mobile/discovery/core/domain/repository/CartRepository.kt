package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

/**
 * Port for the session-scoped cart basket. Implemented in `core:data` as an in-memory singleton (the
 * basket is ephemeral session state, cleared on order placement or off-Home inactivity).
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
}
