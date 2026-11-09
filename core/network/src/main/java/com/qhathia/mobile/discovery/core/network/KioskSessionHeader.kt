package com.qhathia.mobile.discovery.core.network

/**
 * Header that names the current customer session on `GET /api/v1/products/{id}` (api-contract §24):
 * biz records the `PRODUCT_VIEW` with it, so "Clientes usaron la tablet" counts each customer once.
 * Max 200 characters on the server.
 */
const val KIOSK_SESSION_HEADER = "X-Kiosk-Session"
