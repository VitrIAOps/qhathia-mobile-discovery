package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.AuthApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the Qhathia session access token used to authenticate authenticated requests.
 *
 * Once the device is paired, the persisted DEVICE token (from the store session) is the source of the
 * access token. Until then it falls back to logging in with the development credentials (build-config)
 * and caching the token in memory. This is the single seam between the dev-login bootstrap and pairing.
 */
@Singleton
class SessionTokenProvider @Inject constructor(
    private val authApiService: AuthApiService,
    private val storeSessionRepository: StoreSessionRepository,
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = null

    /** Returns a valid access token: the persisted device token when paired, else a dev login. */
    suspend fun accessToken(forceRefresh: Boolean = false): Result<String> {
        if (!forceRefresh) {
            storeSessionRepository.deviceAccessToken()?.let { return Result.success(it) }
            cachedToken?.let { return Result.success(it) }
        }
        return mutex.withLock {
            if (!forceRefresh) {
                storeSessionRepository.deviceAccessToken()?.let { return@withLock Result.success(it) }
                cachedToken?.let { return@withLock Result.success(it) }
            }
            when (val result = authApiService.login(
                email = BuildConfig.QHATHIA_DEV_EMAIL,
                password = BuildConfig.QHATHIA_DEV_PASSWORD,
            )) {
                is NetworkResult.Success -> {
                    cachedToken = result.data.accessToken
                    Result.success(result.data.accessToken)
                }
                is NetworkResult.HttpError ->
                    Result.failure(SttException(SttErrorCategory.Generic))
                is NetworkResult.NetworkError ->
                    Result.failure(SttException(SttErrorMapper.fromNetworkError(result.kind)))
            }
        }
    }

    /** Drops the cached token (e.g. after a 401 from the token endpoint). */
    fun clear() {
        cachedToken = null
    }
}
