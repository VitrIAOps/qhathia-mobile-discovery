package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of a product-search failure, selecting the recovery UX.
 *
 * - [Network] — lost/unstable connectivity: the screen is kept and the established connection message
 *   is shown; no alternative search is run.
 * - [SearchApi] — the search backend failed or the request timed out: treated as a search failure,
 *   surfacing the unhappy screen with a disguised alternative search.
 */
enum class SearchErrorCategory { Network, SearchApi }
