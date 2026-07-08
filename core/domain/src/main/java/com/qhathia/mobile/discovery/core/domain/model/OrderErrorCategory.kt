package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of an order-creation failure. The consumer-facing copy is intentionally the same
 * simplified message for every category (the kiosk hides the underlying cause); the distinction is
 * kept only so the data layer can map failures meaningfully and tests can assert the mapping.
 *
 * - [Network] — lost/unstable connectivity.
 * - [OrderApi] — the order backend failed, rejected the request, or the request timed out.
 */
enum class OrderErrorCategory { Network, OrderApi }
