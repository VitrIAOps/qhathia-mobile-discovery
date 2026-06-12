package com.qhathia.mobile.discovery.core.domain

import com.qhathia.mobile.discovery.core.domain.model.Product

/**
 * Single source of truth for the catalog screen's timing and layout budgets (business + technical
 * rules), shared by the presentation layer.
 */
object CatalogConstants {

    /** Maximum length of the catalog search-bar query, in characters (business rule; aligned with search). */
    const val MAX_QUERY_CHARS: Int = ProductSearchConstants.MAX_QUERY_CHARS

    /** The network-error screen returns to Home idle after this many seconds of inactivity (business rule). */
    const val ERROR_IDLE_TIMEOUT_SECONDS: Long = 30

    /** Maximum number of rows each category band lays out before scrolling horizontally (UX rule). */
    const val MAX_CATEGORY_ROWS: Int = 2

    /**
     * Predicate for the synthetic "Descuentos" section. The catalog endpoint exposes no discount data,
     * only [Product.allowDiscountInquiry] ("the consumer may ask about discounts"); the Descuentos
     * section aggregates products where this is true. This is an approximation of "has an active
     * discount" — see the change design doc's open question.
     */
    fun isDiscountCandidate(product: Product): Boolean = product.allowDiscountInquiry
}
