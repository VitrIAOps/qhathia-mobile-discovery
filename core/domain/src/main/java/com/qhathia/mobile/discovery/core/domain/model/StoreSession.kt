package com.qhathia.mobile.discovery.core.domain.model

/**
 * Values that describe the store (merchant) the device is operating for and that the app needs
 * across screens. Persisted locally and observed by the presentation layer.
 *
 * @property storeName the operating store's display name, or `null` when not yet known.
 * @property deviceAccessToken the DEVICE JWT obtained from pairing, or `null` when not paired. When
 *   present, it is the source of the app's authenticated access token (replaces the dev-credential
 *   login).
 * @property deviceName the paired device's display name, or `null` when not paired.
 */
data class StoreSession(
    val storeName: String? = null,
    val deviceAccessToken: String? = null,
    val deviceName: String? = null,
) {
    /** Whether the device has completed pairing (a device token is persisted). */
    val isPaired: Boolean
        get() = !deviceAccessToken.isNullOrBlank()

    companion object {
        /** Well-defined empty session, returned before any value has been written. */
        val EMPTY: StoreSession = StoreSession()
    }
}
