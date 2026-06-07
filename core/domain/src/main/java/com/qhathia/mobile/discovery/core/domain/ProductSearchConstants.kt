package com.qhathia.mobile.discovery.core.domain

/**
 * Single source of truth for the product-search timing and layout budgets (business + technical
 * rules), shared by the presentation state machine and the network layer.
 */
object ProductSearchConstants {
    /** Maximum length of a search query, in characters (business rule; may change in the future). */
    const val MAX_QUERY_CHARS: Int = 500

    /** Maximum wait for a search response before treating it as failed, in seconds (technical rule). */
    const val SEARCH_TIMEOUT_SECONDS: Long = 30

    /** After this many seconds without a search response, show the slow-search message. */
    const val SLOW_SEARCH_SECONDS: Long = 20

    /** The unhappy (no-results / search-error) screen returns to Home idle after this many seconds. */
    const val UNHAPPY_IDLE_TIMEOUT_SECONDS: Long = 30

    /** Number of alternative products shown on the unhappy screen (business rule). */
    const val ALTERNATIVES_COUNT: Int = 3

    /** Maximum number of rows the results grid lays out before scrolling horizontally (UX rule). */
    const val MAX_RESULT_ROWS: Int = 2
}
