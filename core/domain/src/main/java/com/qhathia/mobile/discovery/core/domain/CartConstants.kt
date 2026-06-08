package com.qhathia.mobile.discovery.core.domain

/**
 * Single source of truth for the cart's business + technical rules: the off-Home inactivity budget,
 * the stock-derived quantity caps, and the consumer-data validation rules.
 */
object CartConstants {

    /** The basket is cleared after this many seconds of inactivity on any screen other than Home. */
    const val INACTIVITY_TIMEOUT_SECONDS: Long = 60

    // TODO(inventory-stock): the product API exposes only an availability enum, not a unit count, so
    // the per-product cap is a placeholder mapping. Replace these with the real stock count when the
    // API exposes it (see openspec change add-shopping-cart, design.md → Open Questions).
    /** Cap applied to a freely-available product until a real stock count exists. */
    const val DEFAULT_MAX_QUANTITY: Int = 99

    /** Cap applied to a low-stock product until a real stock count exists. */
    const val LOW_STOCK_MAX_QUANTITY: Int = 5

    /** Cap applied to a consult-availability product until a real stock count exists. */
    const val CONSULT_MAX_QUANTITY: Int = 1

    /**
     * General mobile-number format. Defaults to the Peruvian mobile rule: 9 digits beginning with `9`,
     * optionally prefixed by `+51`/`51`. Centralized here so it can be widened for other countries.
     */
    val PHONE_REGEX: Regex = Regex("^(?:\\+?51)?9\\d{8}$")

    /**
     * Resolves the stock-derived quantity cap for a product from its API [availability] status.
     * TODO(inventory-stock): replace with the real stock count when available.
     */
    fun maxQuantityFor(availability: String): Int = when (availability.uppercase()) {
        "AVAILABLE" -> DEFAULT_MAX_QUANTITY
        "LOW_STOCK" -> LOW_STOCK_MAX_QUANTITY
        "CONSULT" -> CONSULT_MAX_QUANTITY
        else -> DEFAULT_MAX_QUANTITY
    }
}
