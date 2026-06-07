package com.qhathia.mobile.discovery.core.domain

/**
 * Single source of truth for the product-detail timing and layout budgets, shared by the presentation
 * layer and the network layer.
 */
object ProductDetailConstants {
    /** Maximum wait for a detail response before treating it as failed, in seconds (technical rule). */
    const val DETAIL_TIMEOUT_SECONDS: Long = 30

    /** Number of lateral thumbnail slots shown next to the main image (UX rule from the Figma). */
    const val LATERAL_THUMBNAIL_SLOTS: Int = 3
}
