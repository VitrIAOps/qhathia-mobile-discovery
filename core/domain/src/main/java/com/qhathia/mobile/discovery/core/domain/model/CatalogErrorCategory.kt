package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of a catalog-load failure, selecting the recovery UX.
 *
 * - [Network] — lost/unstable connectivity: the catalog shows the established connection-lost screen.
 * - [Generic] — the catalog backend failed or the request timed out. The catalog has a single error
 *   screen, so this currently surfaces the same connection-lost screen; the distinct category is kept
 *   so the presentation layer can diverge later without a domain change.
 */
enum class CatalogErrorCategory { Network, Generic }
