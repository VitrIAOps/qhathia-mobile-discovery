package com.qhathia.mobile.discovery.core.domain.model

/**
 * The outcome of a product search.
 *
 * @property products the matching products, ordered by relevance (may be empty).
 * @property displayText short headline for the results (e.g. "Encontré 8 taladros para ti").
 * @property understood whether the backend understood the query.
 * @property noResults whether the search produced no products.
 * @property fallbackMessage optional guidance text when the query was ambiguous or empty.
 */
data class ProductSearchResult(
    val products: List<Product>,
    val displayText: String,
    val understood: Boolean,
    val noResults: Boolean,
    val fallbackMessage: String?,
)
