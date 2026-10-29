package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.data.order.toApiMobileNumber
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.network.dto.ShareCartItemRequestDto
import com.qhathia.mobile.discovery.core.network.dto.ShareCartRequestDto
import com.qhathia.mobile.discovery.core.network.dto.ShareCartResponseDto

/**
 * Builds the `POST /api/v1/carts/share-pdf` request from the consumer data and the basket lines. The
 * request body matches the order request, so the phone normalization ([String.toApiMobileNumber]) is
 * reused from `OrderMappers` — the `^9\d{8}$` rule lives in exactly one place.
 */
internal fun buildShareCartRequest(
    name: String,
    phone: String,
    items: List<CartItem>,
): ShareCartRequestDto = ShareCartRequestDto(
    customerName = name.trim(),
    customerPhone = phone.toApiMobileNumber(),
    items = items.map { ShareCartItemRequestDto(productId = it.productId, quantity = it.quantity) },
)

/**
 * Maps a `201` share answer to its outcome. Only `SENT` is a delivery; `FAILED`, `STUB` and any
 * unknown status mean WhatsApp did not receive the quote.
 */
internal fun ShareCartResponseDto.toOutcome(): ShareOutcome =
    if (status.equals(STATUS_SENT, ignoreCase = true)) {
        ShareOutcome.Delivered(pdfUrl)
    } else {
        ShareOutcome.NotDelivered(pdfUrl, status)
    }

private const val STATUS_SENT = "SENT"
