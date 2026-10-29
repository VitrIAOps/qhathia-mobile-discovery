package com.qhathia.mobile.discovery.feature.productdetail.model

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerNameError
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerPhoneError
import com.qhathia.mobile.discovery.core.ui.component.SubmissionLine
import kotlinx.collections.immutable.ImmutableList

/**
 * Immutable snapshot of the product-detail screen.
 *
 * @property status the current detail load status (loading / loaded / error).
 * @property relatedProducts the related-products ("Podrías acompañar este producto con…") sub-state,
 *   loaded independently of [status] so a suggestions failure never blocks the detail.
 * @property query the current bottom search-bar text for a follow-up search.
 * @property showCloseConfirmDialog whether the close-current-search confirmation dialog is visible.
 * @property quantity the quantity selected in the add-to-cart stepper (≥ 1, bounded by stock).
 * @property cartItemCount total items in the cart, shown on the Ir al carrito control's badge.
 * @property share the "Compartir por WhatsApp" dialog sub-state (hidden until the button is tapped).
 */
data class ProductDetailUiState(
    val status: Status = Status.Loading,
    val relatedProducts: RelatedProducts = RelatedProducts.Loading,
    val query: String = "",
    val showCloseConfirmDialog: Boolean = false,
    val quantity: Int = 1,
    val cartItemCount: Int = 0,
    val share: ShareUiState = ShareUiState(),
) {

    /**
     * The share-quote dialog state. Mirrors the cart's consumer form + submission phase so the
     * product-detail share reuses the same loading/success/error overlays from `core:ui`.
     *
     * @property visible whether the share dialog is shown.
     * @property name the consumer's entered name.
     * @property phone the consumer's entered cell-phone number.
     * @property nameError the name validation error, or `null` when valid.
     * @property phoneError the phone validation error, or `null` when valid.
     * @property isShareEnabled whether the consumer data is complete and valid (gates "Compartir").
     * @property submission the submission phase overlaid on the dialog (loading / success / error).
     */
    data class ShareUiState(
        val visible: Boolean = false,
        val name: String = "",
        val phone: String = "",
        val nameError: ConsumerNameError? = ConsumerNameError.Empty,
        val phoneError: ConsumerPhoneError? = ConsumerPhoneError.Empty,
        val isShareEnabled: Boolean = false,
        val submission: ShareSubmission = ShareSubmission.Idle,
    ) {
        /** True while the share request is in flight — the dialog blocks all inputs. */
        val isSubmitting: Boolean get() = submission is ShareSubmission.Submitting

        /** Show the name error only once the field has content (a blank field is not "wrong" yet). */
        val showNameError: Boolean get() = name.isNotBlank() && nameError != null

        /** Show the phone error only once the field has content. */
        val showPhoneError: Boolean get() = phone.isNotBlank() && phoneError != null
    }

    /** The share-submission phase, mirroring the cart's `OrderSubmission`. */
    sealed interface ShareSubmission {

        /** No share in progress — the form is interactive. */
        data object Idle : ShareSubmission

        /** A share request is in flight — inputs are blocked behind a loader. */
        data object Submitting : ShareSubmission

        /** The quote was sent — [summary] holds the shared product for the confirmation screen. */
        data class Success(val summary: ImmutableList<SubmissionLine>) : ShareSubmission

        /** The share was rejected or failed after retrying — the simplified error screen is shown. */
        data object Error : ShareSubmission

        /**
         * The server answered but WhatsApp did not receive the quote (`FAILED` / `STUB`). Shown as an
         * error that sends the consumer to the seller; never as a success.
         */
        data object NotDelivered : ShareSubmission
    }
    /** The product-detail load status. */
    sealed interface Status {

        /** The detail request is in flight. */
        data object Loading : Status

        /** The detail loaded successfully. */
        data class Loaded(val detail: ProductDetail) : Status

        /** The detail request failed; [kind] selects the recovery UX. */
        data class Error(val kind: ErrorKind) : Status
    }

    /** The related-products section state, resolved independently of the main detail. */
    sealed interface RelatedProducts {

        /** The suggestions request is in flight. */
        data object Loading : RelatedProducts

        /** Suggestions loaded with at least one product. */
        data class Loaded(val items: ImmutableList<Product>) : RelatedProducts

        /** No related products to show — the response was empty or the request failed. */
        data object Empty : RelatedProducts
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
