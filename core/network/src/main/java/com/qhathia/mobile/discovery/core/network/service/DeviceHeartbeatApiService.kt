package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatRequestDto
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatResponseDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

/**
 * DEVICE heartbeat (api-contract.md §5): reports telemetry and receives `settingsVersion` and
 * `pendingCommands`. Authenticated with the DEVICE token; folded into a [NetworkResult].
 */
class DeviceHeartbeatApiService @Inject constructor(
    private val client: HttpClient,
) {
    /** `POST /api/v1/devices/heartbeat`. */
    suspend fun sendHeartbeat(
        deviceToken: String,
        body: HeartbeatRequestDto,
    ): NetworkResult<HeartbeatResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}$HEARTBEAT_PATH") {
                bearerAuth(deviceToken)
                contentType(ContentType.Application.Json)
                setBody(body)
                // The shared client allows 120 s for uploads; a heartbeat must give up well before
                // the next tick so a hung request cannot stall the loop.
                timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            }.body<HeartbeatResponseDto>()
        }.toNetworkResult()

    internal companion object {
        const val HEARTBEAT_PATH = "/api/v1/devices/heartbeat"
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
