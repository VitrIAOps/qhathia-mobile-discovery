package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request body of `POST /api/v1/carts/share-pdf` (api-contract §9.5): the consumer's name + normalized
 * mobile number and one line per cart product. Byte-for-byte identical to the order request, but kept
 * as its own type in case the two endpoints diverge.
 */
@Serializable
data class ShareCartRequestDto(
    val customerName: String,
    val customerPhone: String,
    val items: List<ShareCartItemRequestDto>,
)

@Serializable
data class ShareCartItemRequestDto(
    val productId: String,
    val quantity: Int,
)

/**
 * Response of `POST /api/v1/carts/share-pdf`. The endpoint is fail-soft: a `201` always carries a
 * [pdfUrl] even when [status] is `FAILED` (WhatsApp delivery failed but the PDF was generated).
 * Only the fields the client uses are modeled; `ignoreUnknownKeys` drops the rest.
 */
@Serializable
data class ShareCartResponseDto(
    val status: String = "",
    val messageId: String? = null,
    val pdfUrl: String = "",
    val error: String? = null,
)
