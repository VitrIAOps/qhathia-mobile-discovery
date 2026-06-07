package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed product-search [Result], tagging it with the [SearchErrorCategory] so
 * the presentation layer can choose the recovery UX (keep-screen network message vs. unhappy screen
 * with alternatives) without inspecting low-level causes.
 */
class SearchException(val category: SearchErrorCategory) :
    Exception("Product search failed: $category")
