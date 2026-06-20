package com.qhathia.mobile.discovery.core.domain.model

/**
 * The outcome of a product search.
 *
 * @property products the matching products, ordered by relevance (may be empty).
 * @property displayText short headline for the results (e.g. "Encontré 8 taladros para ti").
 * @property understood whether the backend understood the query.
 * @property noResults whether the search produced no products.
 * @property fallbackMessage optional guidance text when the query was ambiguous or empty.
 * @property filters refinement criteria with their options (empty on the unhappy / no-results path).
 * @property suggestions short phrases to refine the search (empty on the unhappy / no-results path).
 */
data class ProductSearchResult(
    val products: List<Product>,
    val displayText: String,
    val understood: Boolean,
    val noResults: Boolean,
    val fallbackMessage: String?,
    val filters: List<SearchFilter> = emptyList(),
    val suggestions: List<String> = emptyList(),
)
