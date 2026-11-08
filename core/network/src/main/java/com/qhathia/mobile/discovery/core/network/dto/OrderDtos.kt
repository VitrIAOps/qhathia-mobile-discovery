package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Request body of `POST /api/v1/orders` (api-contract §9): the consumer's name + normalized mobile
 * number and one line per cart product. `sessionId` is the current customer session (the same id the
 * search and the product views carry), so biz records `ORDER_CREATED` for that customer. `customerNote`
 * is optional and omitted by the kiosk.
 */
@Serializable
data class CreateOrderRequestDto(
    val customerName: String,
    val customerPhone: String,
    val items: List<OrderItemRequestDto>,
    val sessionId: String? = null,
    val customerNote: String? = null,
)

/**
 * One order line. [expectedUnitPrice] is the unit price the customer saw (2 decimals); when it differs
 * from the current price the server answers `409 PRICE_CHANGED` and creates nothing (api-contract §38).
 */
@Serializable
data class OrderItemRequestDto(
    val productId: String,
    val quantity: Int,
    val expectedUnitPrice: Double? = null,
)

/**
 * Response of `POST /api/v1/orders` (the created `OrderResponse`). Only the fields the client renders
 * are modeled; `ignoreUnknownKeys` drops the rest (timestamps, `rejectionReason`, etc.).
 */
@Serializable
data class OrderResponseDto(
    val id: String = "",
    val storeId: String = "",
    val status: String = "",
    val source: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val items: List<OrderItemResponseDto> = emptyList(),
    val sessionId: String? = null,
    val customerNote: String? = null,
)

@Serializable
data class OrderItemResponseDto(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPriceSnapshot: Double = 0.0,
    val lineTotal: Double = 0.0,
)
