package com.qhathia.mobile.discovery.core.domain.model

import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants

/**
 * A search-refinement criterion and the options the consumer can pick to narrow the results
 * (e.g. `criterion = "Voltaje"`, `options = ["12V", "18V", "20V"]`). Framework-free domain model.
 *
 * @property criterion the human-readable dimension name (e.g. "Marca", "Voltaje").
 * @property options the selectable option labels under this criterion.
 * @property values the value to send for each option (`values[i]` ↔ `options[i]`); defaults to the labels.
 */
data class SearchFilter(
    val criterion: String,
    val options: List<String>,
    val values: List<String> = options,
)

/**
 * A single selected filter option, identifying which [criterion]'s [option] the consumer picked.
 * Used as the selection key and as the input to [StructuredSearchFilters.from].
 */
data class SearchFilterOption(
    val criterion: String,
    val option: String,
)

/**
 * The selected chips as the structured filters the search accepts (api-contract §41), sent next to
 * the original text instead of concatenated into it.
 *
 * @property brands labels of the selected "Marca" options.
 * @property tags values of the selected options of every other criterion.
 */
data class StructuredSearchFilters(
    val brands: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = brands.isEmpty() && tags.isEmpty()

    companion object {
        /**
         * Builds the filters for [selected] against the [panel] they were picked from: an option of
         * the brand criterion goes into [brands] as its label; any other option goes into [tags] as
         * its matching value. An option that is not in the panel is dropped, never sent as a label.
         */
        fun from(selected: Collection<SearchFilterOption>, panel: List<SearchFilter>): StructuredSearchFilters {
            val brands = mutableListOf<String>()
            val tags = mutableListOf<String>()
            for (option in selected) {
                val criterion = panel.firstOrNull { it.criterion == option.criterion } ?: continue
                val index = criterion.options.indexOf(option.option)
                if (index < 0) continue
                if (criterion.criterion.equals(ProductSearchConstants.BRAND_CRITERION, ignoreCase = true)) {
                    brands += option.option
                } else {
                    tags += criterion.values.getOrNull(index) ?: option.option
                }
            }
            return StructuredSearchFilters(brands.distinct(), tags.distinct())
        }
    }
}
