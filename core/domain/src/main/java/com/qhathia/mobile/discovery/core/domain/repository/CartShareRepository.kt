package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome

/**
 * Port for sharing a basket as a WhatsApp quote. Implemented in `core:data` against
 * `POST /api/v1/carts/share-pdf`. This does NOT create an order. One call is one request: the retry
 * policy lives in [com.qhathia.mobile.discovery.core.domain.usecase.ShareQuoteUseCase].
 */
interface CartShareRepository {

    /** Shares [items] as a quote for the consumer's [name] and [phone] and classifies the answer. */
    suspend fun shareCart(name: String, phone: String, items: List<CartItem>): ShareOutcome
}
