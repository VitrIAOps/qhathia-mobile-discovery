package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import javax.inject.Inject

/** What the store says about a product right now (fix-unsellable-product-detail, F-32). */
sealed interface ProductCheck {

    /** The product exists; [detail] carries its current availability, `active` flag and stock. */
    data class Current(val detail: ProductDetail) : ProductCheck

    /** The product no longer exists for the tablet (deleted). */
    data object Gone : ProductCheck
}

/**
 * Reads a product again before it is added to the cart or shared, so the detail never acts on a
 * product the store stopped selling while it was open. Returns `null` when the store could not be read
 * (no connection, server error): the caller goes on as before and the cart rechecks when it opens.
 *
 * Uses `GET /products/{id}`, which today records a `PRODUCT_VIEW` for the tablet; accepted until the
 * backend offers a way to skip the count (design decision 1).
 */
class CheckProductOrderableUseCase @Inject constructor(
    private val repository: ProductDetailRepository,
) {
    suspend operator fun invoke(productId: String): ProductCheck? =
        repository.getProduct(productId).fold(
            onSuccess = { ProductCheck.Current(it) },
            onFailure = { if ((it as? DetailException)?.category == DetailErrorCategory.NotFound) ProductCheck.Gone else null },
        )
}
