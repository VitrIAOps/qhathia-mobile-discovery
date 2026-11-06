package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import kotlinx.coroutines.flow.Flow

/**
 * Port for the persisted store (merchant) session. Implemented in `core:data`; a future device
 * pairing / switch-store flow will feed the same port without changing consumers.
 */
interface StoreSessionRepository {

    /** Emits the current [StoreSession] immediately and again whenever any value changes. */
    fun observe(): Flow<StoreSession>

    /** Persists [storeName] as the active store's name. */
    suspend fun updateStoreName(storeName: String?)

    /** Reads the persisted DEVICE access token, or `null` when the device is not paired. */
    suspend fun deviceAccessToken(): String?

    /** Persists the DEVICE access token, device name and the pairing code used, produced by pairing. */
    suspend fun saveDeviceSession(deviceAccessToken: String, deviceName: String?, pairingCode: String? = null)

    /** Clears the persisted device session, pairing code included (used by unpair). */
    suspend fun clearDeviceSession()

    /**
     * Revokes the device session after the tablet was unlinked: in one write, removes the device token,
     * device name, pairing code and store name, and marks the unlink notice as pending.
     */
    suspend fun revokeDeviceSession()

    /** Clears the pending unlink notice and nothing else. */
    suspend fun dismissUnlinkedNotice()
}
