package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.SearchFilterOption
import javax.inject.Inject

/**
 * Builds the natural-language re-query string for the search-refinement interaction by concatenating
 * the selected filter terms and the optional selected suggestion onto the immutable base query.
 *
 * Deterministic and framework-free so it is trivially unit-testable, mirroring [GeneralizeQueryUseCase].
 * The join rule:
 * 1. A filter option renders as `"<criterion> <option>"` lowercased (e.g. "voltaje 12v").
 * 2. The suggestion appends as its phrase, lowercased (e.g. "para uso doméstico").
 * 3. Parts join with single spaces onto the trimmed base query, in the order: base, filters, suggestion.
 * 4. When nothing is selected the trimmed base query is returned unchanged.
 */
class ComposeSearchQueryUseCase @Inject constructor() {

    operator fun invoke(
        baseQuery: String,
        selectedFilters: List<SearchFilterOption>,
        selectedSuggestion: String?,
    ): String {
        val base = baseQuery.trim()
        val terms = buildList {
            selectedFilters.forEach { add("${it.criterion} ${it.option}".trim().lowercase()) }
            selectedSuggestion?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it.lowercase()) }
        }.filter { it.isNotEmpty() }

        if (terms.isEmpty()) return base
        return (listOf(base).filter { it.isNotEmpty() } + terms).joinToString(separator = " ")
    }
}
