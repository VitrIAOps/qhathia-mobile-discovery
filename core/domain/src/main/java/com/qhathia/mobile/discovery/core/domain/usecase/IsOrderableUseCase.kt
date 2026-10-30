package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import javax.inject.Inject

/**
 * Whether a product can be added to the cart, ordered or shared. Mirrors the rule the server enforces
 * on `POST /orders` and `POST /carts/share-pdf`: the product must be `AVAILABLE` and active. List
 * endpoints do not send `active`, so `null` means "not known to be inactive" and is allowed.
 */
class IsOrderableUseCase @Inject constructor() {

    operator fun invoke(availability: String, active: Boolean? = null): Boolean =
        isOrderable(availability, active)

    companion object {
        private const val AVAILABLE = "AVAILABLE"

        /** The rule itself, for callers that cannot inject (pure models, UI mapping). */
        fun isOrderable(availability: String, active: Boolean? = null): Boolean =
            availability.equals(AVAILABLE, ignoreCase = true) && active != false
    }
}

/** Whether this search / catalog / related product can be added to the cart. */
val Product.isOrderable: Boolean get() = IsOrderableUseCase.isOrderable(availability)

/** Whether this product detail can be added to the cart or shared. */
val ProductDetail.isOrderable: Boolean get() = IsOrderableUseCase.isOrderable(availability, active)
