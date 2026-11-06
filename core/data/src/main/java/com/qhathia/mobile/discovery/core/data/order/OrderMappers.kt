package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.model.OrderItem
import com.qhathia.mobile.discovery.core.domain.model.toMoney
import com.qhathia.mobile.discovery.core.network.dto.CreateOrderRequestDto
import com.qhathia.mobile.discovery.core.network.dto.OrderItemRequestDto
import com.qhathia.mobile.discovery.core.network.dto.OrderItemResponseDto
import com.qhathia.mobile.discovery.core.network.dto.OrderResponseDto

/**
 * Normalizes a consumer-entered cell-phone to the API's required Peruvian mobile format (`^9\d{8}$`):
 * strips whitespace and any leading `+51`/`51` country prefix. The form validation ([CartConstants]
 * `PHONE_REGEX`) already guarantees the input is one of `9XXXXXXXX`, `519XXXXXXXX`, or `+519XXXXXXXX`
 * (optionally spaced), so the result is always the bare 9-digit number.
 */
internal fun String.toApiMobileNumber(): String =
    filterNot { it.isWhitespace() }
        .removePrefix("+")
        .removePrefix("51")

/**
 * Builds the `POST /api/v1/orders` request from the consumer data and the basket lines. Each line
 * carries the unit price the customer saw (`expectedUnitPrice`, 2 decimals half up), so the server
 * answers `409 PRICE_CHANGED` instead of recording a price the customer never saw.
 */
internal fun buildCreateOrderRequest(
    name: String,
    phone: String,
    items: List<CartItem>,
): CreateOrderRequestDto = CreateOrderRequestDto(
    customerName = name.trim(),
    customerPhone = phone.toApiMobileNumber(),
    items = items.map {
        OrderItemRequestDto(
            productId = it.productId,
            quantity = it.quantity,
            expectedUnitPrice = it.priceUnit.toMoney().toDouble(),
        )
    },
)

/** Maps the created-order response DTO to the domain [Order]. */
internal fun OrderResponseDto.toDomain(): Order = Order(
    id = id,
    status = status,
    subtotal = subtotal,
    total = total,
    customerName = customerName,
    customerPhone = customerPhone,
    items = items.map { it.toDomain() },
)

/** Maps an order-item response DTO to the domain [OrderItem]. */
internal fun OrderItemResponseDto.toDomain(): OrderItem = OrderItem(
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPriceSnapshot = unitPriceSnapshot,
    lineTotal = lineTotal,
)
