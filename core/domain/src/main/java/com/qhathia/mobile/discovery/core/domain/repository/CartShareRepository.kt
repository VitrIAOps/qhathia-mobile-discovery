package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShare

/**
 * Port for sharing the cart as a WhatsApp quote. Implemented in `core:data` against
 * `POST /api/v1/carts/share-pdf`. The consumer name/phone come from the cart form and the lines from
 * the basket. This does NOT create an order — it requests a quote PDF delivered to the consumer's
 * WhatsApp.
 */
interface CartShareRepository {

    /**
     * Shares [items] as a WhatsApp quote for the consumer's [name] and [phone]. Returns the
     * [CartShare] outcome on success, or a failure carrying a
     * [com.qhathia.mobile.discovery.core.domain.model.CartShareException].
     */
    suspend fun shareCart(name: String, phone: String, items: List<CartItem>): Result<CartShare>
}
