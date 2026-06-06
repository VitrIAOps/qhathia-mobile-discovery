package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.AuthApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the Qhathia session access token used to authenticate the STT-token request.
 *
 * Interim seam: device pairing/auth does not exist yet, so the token is obtained by logging in with
 * the development credentials (build-config) and cached in memory. When pairing lands, this is the
 * single place that changes — callers just ask for [accessToken].
 */
@Singleton
class SessionTokenProvider @Inject constructor(
    private val authApiService: AuthApiService,
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = null

    /** Returns a valid access token, logging in on first use (or when [forceRefresh] is set). */
    suspend fun accessToken(forceRefresh: Boolean = false): Result<String> {
        if (!forceRefresh) cachedToken?.let { return Result.success(it) }
        return mutex.withLock {
            if (!forceRefresh) cachedToken?.let { return@withLock Result.success(it) }
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
