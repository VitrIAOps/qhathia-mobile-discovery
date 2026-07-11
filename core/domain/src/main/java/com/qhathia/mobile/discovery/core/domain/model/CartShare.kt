package com.qhathia.mobile.discovery.core.domain.model

/**
 * Outcome of sharing the cart as a WhatsApp quote via `POST /api/v1/carts/share-pdf`. Framework-free
 * domain model. The endpoint is fail-soft: a `201` always carries [pdfUrl] even when WhatsApp delivery
 * failed, so [status] records the delivery result (`SENT` / `STUB` / `FAILED`) without failing the
 * call. Sharing does NOT create a purchase order.
 *
 * @property status delivery status as returned by the API (`SENT`, `STUB`, or `FAILED`).
 * @property pdfUrl the generated quote PDF's URL (always present on a successful share).
 * @property messageId the WhatsApp message id when the PDF was sent, or `null` otherwise.
 */
data class CartShare(
    val status: String,
    val pdfUrl: String,
    val messageId: String?,
)
