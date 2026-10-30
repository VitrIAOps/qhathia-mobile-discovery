package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of a transient share failure ([ShareOutcome.Failed]). The consumer sees the same
 * message for every category; the split is kept so the data layer can map failures and tests can
 * assert the mapping.
 *
 * - [Network] — lost or unstable connectivity.
 * - [ShareApi] — a timeout, a `5xx`, or an unknown failure of the share backend.
 */
enum class CartShareErrorCategory { Network, ShareApi }
