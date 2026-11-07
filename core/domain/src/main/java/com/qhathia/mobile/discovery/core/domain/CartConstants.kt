package com.qhathia.mobile.discovery.core.domain

/**
 * Single source of truth for the cart's business + technical rules: the customer-session inactivity budget,
 * the stock-derived quantity caps, and the consumer-data validation rules.
 */
object CartConstants {

    /**
     * The customer session ends after this many seconds without activity while the cart dialog is
     * closed (customer-session capability): on any screen other than Home and onboarding, and on Home
     * while the basket is not empty. Ending the session clears the basket and the consumer forms,
     * closes the overlays and returns Home. Touches inside dialogs and typing count as activity.
     */
    const val INACTIVITY_TIMEOUT_SECONDS: Long = 60

    /**
     * The inactivity budget while the cart dialog is open (cart, consumer form, or an order or share
     * result): a customer reviewing the basket or reading the confirmation is given longer, while an
     * abandoned cart still resets (F-35).
     */
    const val CART_DIALOG_INACTIVITY_SECONDS: Long = 180

    /**
     * Most distinct products the basket holds: the limit `POST /orders` and `POST /carts/share-pdf`
     * accept (`items` up to 20 entries; more is a `422`). More units of a line already in the basket
     * are still allowed up to its quantity cap.
     */
    const val MAX_DISTINCT_LINES: Int = 20

    /**
     * Most units of one product per line: the `quantity` limit of `POST /orders` (`@Max(99)`), and the
     * cap of an `AVAILABLE` product without a stock count.
     */
    const val DEFAULT_MAX_QUANTITY: Int = 99

    /** Cap of a `LOW_STOCK` product without a stock count. */
    const val LOW_STOCK_MAX_QUANTITY: Int = 5

    /** Cap of a `CONSULT` ("a consultar") product without a stock count. */
    const val CONSULT_MAX_QUANTITY: Int = 1

    /** A stock count up to this many units is shown as "Pocas unidades (quedan N)". */
    const val LOW_STOCK_LABEL_MAX: Int = 5

    /** Longest consumer name `POST /orders` and `POST /carts/share-pdf` accept (`@Size(max = 200)`). */
    const val MAX_CONSUMER_NAME_CHARS: Int = 200

    /**
     * General mobile-number format. Defaults to the Peruvian mobile rule: 9 digits beginning with `9`,
     * optionally prefixed by `+51`/`51`. Centralized here so it can be widened for other countries.
     */
    val PHONE_REGEX: Regex = Regex("^(?:\\+?51)?9\\d{8}$")

    /**
     * The quantity cap of a product: its [stock] when the product has a count (at least 1, at most
     * [DEFAULT_MAX_QUANTITY]), otherwise the cap derived from its [availability].
     */
    fun maxQuantityFor(availability: String, stock: Int?): Int =
        (stock ?: maxQuantityFor(availability)).coerceIn(1, DEFAULT_MAX_QUANTITY)

    /** The cap of a product without a stock count, derived from its API [availability] status. */
    fun maxQuantityFor(availability: String): Int = when (availability.uppercase()) {
        "AVAILABLE" -> DEFAULT_MAX_QUANTITY
        "LOW_STOCK" -> LOW_STOCK_MAX_QUANTITY
        "CONSULT" -> CONSULT_MAX_QUANTITY
        else -> DEFAULT_MAX_QUANTITY
    }
}
