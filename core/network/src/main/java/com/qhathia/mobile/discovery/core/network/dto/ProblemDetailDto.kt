package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * The `application/problem+json` body the backend sends with an HTTP error (api-contract §35–§39).
 * Every field is optional: a service only fills the ones that apply to its error.
 *
 * @property errorCode the machine-readable error, e.g. `DEVICE_UNLINKED`, `PRICE_CHANGED`.
 * @property code legacy name of [errorCode], still sent by some endpoints.
 * @property detail the human-readable explanation.
 * @property reason why a product cannot be sold (`INACTIVE`, `OUT_OF_STOCK`, `CONSULT`, `LOW_STOCK`).
 * @property available units in stock for `STOCK_INSUFFICIENT`.
 * @property requested units asked for `STOCK_INSUFFICIENT`.
 * @property changes per-product price changes for `PRICE_CHANGED`.
 * @property fieldErrors per-field validation messages.
 */
@Serializable
data class ProblemDetailDto(
    val errorCode: String? = null,
    val code: String? = null,
    val detail: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val reason: String? = null,
    val available: Int? = null,
    val requested: Int? = null,
    val changes: List<PriceChangeDto> = emptyList(),
    val fieldErrors: Map<String, String> = emptyMap(),
) {
    /** [errorCode], falling back to the legacy [code]. */
    val resolvedErrorCode: String?
        get() = errorCode ?: code
}

/** One line of a `409 PRICE_CHANGED`: the price the customer saw against the current one. */
@Serializable
data class PriceChangeDto(
    val productId: String = "",
    val productName: String = "",
    val expectedUnitPrice: Double = 0.0,
    val currentUnitPrice: Double = 0.0,
)
