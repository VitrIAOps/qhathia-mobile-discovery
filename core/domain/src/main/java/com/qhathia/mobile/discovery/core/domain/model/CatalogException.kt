package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed catalog [Result], tagging it with the [CatalogErrorCategory] so the
 * presentation layer can choose the recovery UX without inspecting low-level causes.
 */
class CatalogException(val category: CatalogErrorCategory) :
    Exception("Product catalog load failed: $category")
