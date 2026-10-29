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

    /** A deterministic `4xx` answer. Final, never retried. */
    data class Rejected(val kind: ShareRejection) : ShareOutcome

    /** Connectivity loss, timeout or `5xx`. The only outcome that is retried. */
    data class Failed(val category: CartShareErrorCategory) : ShareOutcome
}

/** Why the server rejected a share. */
enum class ShareRejection {
    /** `400`: a product is inactive or not `AVAILABLE`. */
    Unavailable,

    /** `404`: a product does not exist. */
    NotFound,

    /** `422`: the request failed validation (for example more than 20 lines). */
    Invalid,

    /** Any other `4xx`. */
    Other,
}
