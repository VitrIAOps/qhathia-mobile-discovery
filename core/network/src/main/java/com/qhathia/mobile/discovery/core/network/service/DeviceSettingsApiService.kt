package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceSettingsDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import javax.inject.Inject

/** The tablet's own remote settings, authenticated with the DEVICE token (api-contract.md §21). */
class DeviceSettingsApiService @Inject constructor(
    private val client: HttpClient,
) {
    /** `GET /api/v1/devices/me/settings`. */
    suspend fun getMySettings(deviceToken: String): NetworkResult<DeviceSettingsDto> =
        runCatching {
            client.get("${BuildConfig.QHATHIA_BASE_URL}$SETTINGS_PATH") {
                bearerAuth(deviceToken)
                timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            }.body<DeviceSettingsDto>()
        }.toNetworkResult()

    internal companion object {
        const val SETTINGS_PATH = "/api/v1/devices/me/settings"
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
