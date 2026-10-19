package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceCommandAckDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import javax.inject.Inject

/** Remote-command acknowledgement for the DEVICE (api-contract.md §23). */
class DeviceCommandApiService @Inject constructor(
    private val client: HttpClient,
) {
    /** `POST /api/v1/devices/me/commands/{commandId}/ack` → 204. */
    suspend fun ack(
        deviceToken: String,
        commandId: String,
        body: DeviceCommandAckDto,
    ): NetworkResult<Unit> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}$COMMANDS_PATH/${commandId.encodeURLPathPart()}/ack") {
                bearerAuth(deviceToken)
                contentType(ContentType.Application.Json)
                setBody(body)
                timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            }
            Unit
        }.toNetworkResult()

    internal companion object {
        const val COMMANDS_PATH = "/api/v1/devices/me/commands"
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
