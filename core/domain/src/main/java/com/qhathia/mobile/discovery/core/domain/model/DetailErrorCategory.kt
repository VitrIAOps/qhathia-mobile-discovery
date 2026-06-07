package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of a product-detail fetch failure, selecting the recovery UX.
 *
 * - [Network] — lost/unstable connectivity: the established connection message is shown.
 * - [NotFound] — the product does not exist (HTTP 404): the not-found state is shown.
 * - [DetailApi] — the detail backend failed or the request timed out: a generic detail error.
 */
enum class DetailErrorCategory { Network, NotFound, DetailApi }
