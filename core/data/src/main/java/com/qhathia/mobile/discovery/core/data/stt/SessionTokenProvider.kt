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
     * The token cannot be renewed without pairing again, so a `401` is never retried with it. When the
     * backend reports the tablet as unlinked, the HTTP client's unlink signal ends the session.
     */
    suspend fun accessToken(): Result<String> {
        val token = storeSessionRepository.deviceAccessToken()
        return if (!token.isNullOrBlank()) {
            Result.success(token)
        } else {
            Result.failure(SttException(SttErrorCategory.Generic))
        }
    }
}
