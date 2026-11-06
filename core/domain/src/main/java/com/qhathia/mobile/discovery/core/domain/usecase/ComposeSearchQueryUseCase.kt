package com.qhathia.mobile.discovery.core.domain.usecase

import javax.inject.Inject

/**
 * Builds the natural-language re-query string by appending the optional selected suggestion onto the
 * immutable base query. Filter chips are not part of the text: they travel as structured filters
 * ([com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters], F-37).
 *
 * The rule: the suggestion appends lowercased with a single space onto the trimmed base query; with no
 * suggestion the trimmed base query is returned unchanged.
 */
class ComposeSearchQueryUseCase @Inject constructor() {

    operator fun invoke(baseQuery: String, selectedSuggestion: String?): String {
        val base = baseQuery.trim()
        val suggestion = selectedSuggestion?.trim()?.takeIf { it.isNotEmpty() }?.lowercase() ?: return base
        return listOf(base, suggestion).filter { it.isNotEmpty() }.joinToString(separator = " ")
    }
}
