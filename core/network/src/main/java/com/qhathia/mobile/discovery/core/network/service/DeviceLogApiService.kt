package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogsRequestDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

/** Device logs for remote traceability (api-contract.md, `POST /api/v1/devices/logs`), DEVICE token. */
class DeviceLogApiService @Inject constructor(
    private val client: HttpClient,
) {
    /** `POST /api/v1/devices/logs` → 204. */
    suspend fun send(
        deviceToken: String,
        body: DeviceLogsRequestDto,
    ): NetworkResult<Unit> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}$LOGS_PATH") {
                bearerAuth(deviceToken)
                contentType(ContentType.Application.Json)
                setBody(body)
                timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            }
            Unit
        }.toNetworkResult()

    internal companion object {
        const val LOGS_PATH = "/api/v1/devices/logs"
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
