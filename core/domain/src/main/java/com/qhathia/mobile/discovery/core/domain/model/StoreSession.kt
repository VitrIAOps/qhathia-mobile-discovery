package com.qhathia.mobile.discovery.core.domain.model

/**
 * Values that describe the store (merchant) the device is operating for and that the app needs
 * across screens. Persisted locally and observed by the presentation layer.
 *
 * @property storeName the operating store's display name, or `null` when not yet known.
 */
data class StoreSession(
    val storeName: String? = null,
) {
    companion object {
        /** Well-defined empty session, returned before any value has been written. */
        val EMPTY: StoreSession = StoreSession()
    }
}
