package com.qhathia.mobile.discovery.core.domain.model

/**
 * A search-refinement criterion and the options the consumer can pick to narrow the results
 * (e.g. `criterion = "Voltaje"`, `options = ["12V", "18V", "20V"]`). Framework-free domain model.
 *
 * @property criterion the human-readable dimension name (e.g. "Marca", "Voltaje").
 * @property options the selectable option labels under this criterion.
 */
data class SearchFilter(
    val criterion: String,
    val options: List<String>,
)

/**
 * A single selected filter option, identifying which [criterion]'s [option] the consumer picked.
 * Used as the selection key and as the input to [com.qhathia.mobile.discovery.core.domain.usecase.ComposeSearchQueryUseCase].
 */
data class SearchFilterOption(
    val criterion: String,
    val option: String,
)
