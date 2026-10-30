package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of an order-creation failure. The consumer-facing copy is intentionally the same
 * simplified message for every category (the kiosk hides the underlying cause); the distinction is
 * kept only so the data layer can map failures meaningfully and tests can assert the mapping.
 *
 * - [Network] — lost/unstable connectivity.
 * - [OrderApi] — the order backend failed, rejected the request, or the request timed out.
 * - [Unavailable] — `400` or `409` (`PRODUCT_NOT_SELLABLE`): a product in the basket is out of stock, inactive or not `AVAILABLE` any more.
 * - [NotFound] — `404`: a product in the basket does not exist in the store any more.
 *
 * [Unavailable] and [NotFound] make the cart recheck its lines and flag the unavailable ones instead
 * of showing the generic error (CAR-023/025).
 */
enum class OrderErrorCategory { Network, OrderApi, Unavailable, NotFound }
