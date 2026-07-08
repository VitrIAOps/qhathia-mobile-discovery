package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the Qhathia session access token used to authenticate authenticated requests.
 *
 * The persisted DEVICE token obtained from pairing (via the pairing-session token endpoint) is the
 * single source of the access token. There is no login fallback: when the device is not paired there
 * is no token, and callers surface a clean failure rather than authenticating with credentials.
 */
@Singleton
class SessionTokenProvider @Inject constructor(
    private val storeSessionRepository: StoreSessionRepository,
) {

    /**
     * Returns the persisted DEVICE access token, or a failure when the device is not paired.
     *
     * [forceRefresh] is retained for call-site compatibility (repositories re-request a token after a
     * 401). Because the device token is the only source and cannot be silently renewed without
     * re-pairing, a refresh simply re-reads the persisted token.
     */
    suspend fun accessToken(forceRefresh: Boolean = false): Result<String> {
        val token = storeSessionRepository.deviceAccessToken()
        return if (!token.isNullOrBlank()) {
            Result.success(token)
        } else {
            Result.failure(SttException(SttErrorCategory.Generic))
        }
    }

    /**
     * Retained for call-site compatibility after a 401. There is no in-memory token to drop anymore
     * (the device token lives in the persisted session), so this is a no-op.
     */
    fun clear() = Unit
}
