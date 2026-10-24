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
     * Predicate for the synthetic "Descuentos" section: the product has an active discount, i.e. the
     * server sent a [Product.discountedPrice]. Not [Product.allowDiscountInquiry], which only means
     * the consumer may ask about one (BUG-013).
     */
    fun isDiscountCandidate(product: Product): Boolean = product.discountedPrice != null
}
