package com.qhathia.mobile.discovery.feature.productdetail.model

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import kotlinx.collections.immutable.ImmutableList

/**
 * Immutable snapshot of the product-detail screen.
 *
 * @property status the current load status (loading / loaded / error).
 * @property query the current bottom search-bar text for a follow-up search.
 * @property showCloseConfirmDialog whether the close-current-search confirmation dialog is visible.
 * @property quantity the quantity selected in the add-to-cart stepper (≥ 1, bounded by stock).
 * @property cartItemCount total items in the cart, shown on the Ir al carrito control's badge.
 */
data class ProductDetailUiState(
    val status: Status = Status.Loading,
    val query: String = "",
    val showCloseConfirmDialog: Boolean = false,
    val quantity: Int = 1,
    val cartItemCount: Int = 0,
) {
    /** The product-detail load status. */
    sealed interface Status {

        /** The detail request is in flight. */
        data object Loading : Status

        /** The detail loaded successfully, with its mock related products. */
        data class Loaded(
            val detail: ProductDetail,
            val relatedProducts: ImmutableList<Product>,
        ) : Status

        /** The detail request failed; [kind] selects the recovery UX. */
        data class Error(val kind: ErrorKind) : Status
    }

    /** Why the detail failed to load. */
    enum class ErrorKind {
        /** Product does not exist (HTTP 404). */
        NotFound,

        /** Lost/unstable connectivity. */
        Network,

        /** Backend failure or timeout. */
        Generic,
    }
}
