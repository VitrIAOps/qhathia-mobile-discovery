package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed product-detail [Result], tagging it with the [DetailErrorCategory] so
 * the presentation layer can choose the recovery UX (network message vs. not-found vs. generic error)
 * without inspecting low-level causes.
 */
class DetailException(val category: DetailErrorCategory) :
    Exception("Product detail failed: $category")
