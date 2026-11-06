package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceInfoResponseDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceTokenResponseDto
import com.qhathia.mobile.discovery.core.network.dto.GeneratePairingTokenRequestDto
import com.qhathia.mobile.discovery.core.network.dto.PairingTokenResponseDto
import com.qhathia.mobile.discovery.core.network.readProblemDetail
import com.qhathia.mobile.discovery.core.network.toNetworkErrorKind
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Tablet-side device pairing REST calls (api-contract.md §5). The pairing-token and token-exchange
 * endpoints are public (the tablet has no JWT yet); `/devices/me` is authenticated with the DEVICE
 * token obtained from the exchange. Every call folds into a [NetworkResult].
 */
class DevicePairingApiService @Inject constructor(
    private val client: HttpClient,
) {
    /**
     * `POST /api/v1/devices/pairing-token` — generates a code + opaque session (public). With
     * [replacesSessionId], the backend also cancels that earlier session (§37); without it, no body.
     */
    suspend fun generatePairingToken(replacesSessionId: String? = null): NetworkResult<PairingTokenResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}$PAIRING_TOKEN_PATH") {
                if (replacesSessionId != null) {
                    contentType(ContentType.Application.Json)
                    setBody(GeneratePairingTokenRequestDto(replacesSessionId))
                }
            }.body<PairingTokenResponseDto>()
        }.toNetworkResult()

    /** `POST /api/v1/devices/pairing-session/{sessionId}/cancel` — public, idempotent: 204, or 404. */
    suspend fun cancelPairingSession(sessionId: String): NetworkResult<Unit> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}$PAIRING_SESSION_PATH/${sessionId.encodeURLPathPart()}/cancel")
            Unit
        }.toNetworkResult()

    /**
     * `GET /api/v1/devices/pairing-session/{sessionId}/token` — one-shot DEVICE JWT exchange (public).
     * HTTP errors keep the body's `errorCode` (see [PairingExchangeResponse]).
     */
    suspend fun exchangeToken(sessionId: String): PairingExchangeResponse =
        try {
            val dto = client.get("${BuildConfig.QHATHIA_BASE_URL}$PAIRING_SESSION_PATH/$sessionId/token")
                .body<DeviceTokenResponseDto>()
            PairingExchangeResponse.Success(dto)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: ResponseException) {
            PairingExchangeResponse.HttpError(
                code = failure.response.status.value,
                errorCode = failure.readProblemDetail()?.resolvedErrorCode,
            )
        } catch (failure: Exception) {
            PairingExchangeResponse.NetworkError(failure, failure.toNetworkErrorKind())
        }

    /** `GET /api/v1/devices/me` — paired-device info, authenticated with the DEVICE token. */
    suspend fun getPairedDevice(deviceToken: String): NetworkResult<DeviceInfoResponseDto> =
        runCatching {
            client.get("${BuildConfig.QHATHIA_BASE_URL}$DEVICES_ME_PATH") {
                bearerAuth(deviceToken)
            }.body<DeviceInfoResponseDto>()
        }.toNetworkResult()

    private companion object {
        const val PAIRING_TOKEN_PATH = "/api/v1/devices/pairing-token"
        const val PAIRING_SESSION_PATH = "/api/v1/devices/pairing-session"
        const val DEVICES_ME_PATH = "/api/v1/devices/me"
    }
}
