package com.qhathia.mobile.discovery.core.domain.model

/**
 * A single line of a created [Order]. Framework-free. The unit price is the server-side snapshot taken
 * when the order was created, so it does not change if the merchant later re-prices the product.
 *
 * @property productId the ordered product's identifier.
 * @property productName the product name as resolved by the API at creation time.
 * @property quantity the ordered quantity.
 * @property unitPriceSnapshot the per-unit price snapshotted server-side at creation.
 * @property lineTotal the line total (`quantity × unitPriceSnapshot`) as computed server-side.
 */
data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPriceSnapshot: Double,
    val lineTotal: Double,
)
