package com.qhathia.mobile.discovery.core.domain.model

/**
 * A purchase order created from the cart basket. Framework-free domain model mapped from the API
 * response of `POST /api/v1/orders`. Totals are owned by the server (snapshot at creation time); the
 * client never recomputes them.
 *
 * @property id the created order's identifier.
 * @property status the order status as returned by the API (e.g. `PENDING`).
 * @property subtotal the server-computed subtotal.
 * @property total the server-computed total to pay.
 * @property customerName the consumer name echoed back by the API.
 * @property customerPhone the consumer cell-phone echoed back by the API.
 * @property items the ordered lines, each with its server-snapshotted unit price.
 */
data class Order(
    val id: String,
    val status: String,
    val subtotal: Double,
    val total: Double,
    val customerName: String,
    val customerPhone: String,
    val items: List<OrderItem>,
)
