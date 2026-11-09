package com.qhathia.mobile.discovery.core.domain.model

/**
 * Result of sharing a WhatsApp quote via `POST /api/v1/carts/share-pdf`. Sharing never creates an
 * order. The endpoint is fail-soft: a `201` always carries a PDF URL, and its `status` says whether
 * WhatsApp actually received it, so a `201` alone is not a delivery.
 */
sealed interface ShareOutcome {

    /** `status = SENT`: the quote reached the consumer's WhatsApp. The only success. */
    data class Delivered(val pdfUrl: String) : ShareOutcome

    /**
     * `201` with `status = FAILED`, `STUB` or an unknown value: the PDF exists but WhatsApp did not
     * receive it. Final, never retried.
     *
     * @property status the raw status from the server, kept for logs and tests.
     */
    data class NotDelivered(val pdfUrl: String, val status: String) : ShareOutcome

    /**
     * A deterministic `4xx` answer. Final, never retried. When the server named the product
     * (`PRODUCT_NOT_SELLABLE`, `STOCK_INSUFFICIENT`), [productId], [productName] and, for a stock
     * shortage, [available] carry it. [reason] says why a product cannot be sold (`INACTIVE`,
     * `OUT_OF_STOCK`, `CONSULT`, `LOW_STOCK`; api-contract §39), when the server sent it.
     */
    data class Rejected(
        val kind: ShareRejection,
        val productId: String? = null,
        val productName: String? = null,
        val available: Int? = null,
        val reason: String? = null,
    ) : ShareOutcome

    /** Connectivity loss, timeout or `5xx`. The only outcome that is retried. */
    data class Failed(val category: CartShareErrorCategory) : ShareOutcome
}

/** Why the server rejected a share. */
enum class ShareRejection {
    /** `400` or `409` (`PRODUCT_NOT_SELLABLE`): a product is out of stock, inactive or not `AVAILABLE`. */
    Unavailable,

    /** `404`: a product does not exist. */
    NotFound,

    /** `422`: the request failed validation (for example more than 20 lines). */
    Invalid,

    /** `422 STOCK_INSUFFICIENT`: a line asks for more units than the stock. */
    StockInsufficient,

    /** Any other `4xx`. */
    Other,
}
