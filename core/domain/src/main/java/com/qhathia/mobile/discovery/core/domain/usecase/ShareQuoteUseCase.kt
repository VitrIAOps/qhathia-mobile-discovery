package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import javax.inject.Inject

/**
 * Shares a basket as a WhatsApp quote, used by the cart and the product detail. Holds the retry
 * policy: up to [MAX_ATTEMPTS] requests in total, and only while the answer is
 * [ShareOutcome.Failed] (connectivity, timeout, `5xx`). A `4xx` or a `201` that was not delivered is
 * final and sent once. Field validation happens upstream in the forms.
 */
class ShareQuoteUseCase @Inject constructor(
    private val repository: CartShareRepository,
) {
    suspend operator fun invoke(name: String, phone: String, items: List<CartItem>): ShareOutcome {
        var outcome = repository.shareCart(name, phone, items)
        var attempts = 1
        while (outcome is ShareOutcome.Failed && attempts < MAX_ATTEMPTS) {
            outcome = repository.shareCart(name, phone, items)
            attempts++
        }
        return outcome
    }

    companion object {
        /** Total share requests allowed for one share, counting the first one. */
        const val MAX_ATTEMPTS = 3
    }
}
