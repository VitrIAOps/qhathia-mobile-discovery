package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Port for the tablet-side device pairing flow. Implemented in `core:data` over the pairing REST
 * service and status WebSocket; the device token produced by [exchangeForDeviceToken] is persisted in
 * the store session so it survives restarts.
 */
interface DevicePairingRepository {

    /**
     * Generates a fresh code + session (`POST /devices/pairing-token`). The session of the code shown
     * before, kept across restarts, is sent so the backend cancels it (F-02).
     */
    suspend fun generateCode(): Result<PairingCode>

    /**
     * Cancels the code a previous process left on screen, if any, and forgets it (F-02). When the
     * tablet is paired it only forgets it. Best-effort: a failure keeps it for the next [generateCode].
     */
    suspend fun cancelAbandonedSession(): Result<Unit>

    /** Streams pairing status for [sessionId]; completes when the server closes the channel. */
    fun observeStatus(sessionId: String): Flow<PairingStatus>

    /**
     * Exchanges an approved [sessionId] for the DEVICE token, persists it, and returns the paired
     * device identity.
     */
    suspend fun exchangeForDeviceToken(sessionId: String): Result<PairedDevice>

    /** Reads the currently paired device (`GET /devices/me`) using the persisted device token. */
    suspend fun getPairedDevice(): Result<PairedDevice>

    /** Unpairs the device. No backend API exists yet — see the implementation's TODO. */
    suspend fun unpair(): Result<Unit>
}
