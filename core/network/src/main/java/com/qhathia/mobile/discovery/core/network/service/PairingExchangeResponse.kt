package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.dto.DeviceTokenResponseDto

/**
 * Wire-level result of `GET /devices/pairing-session/{id}/token`. Unlike `NetworkResult`, an HTTP
 * error keeps the body's `errorCode`, because 409 and 410 each have two meanings
 * (api-contract.md §5): `PAIRING_NOT_APPROVED` / `PAIRING_REJECTED` and `PAIRING_TOKEN_EXPIRED` /
 * `PAIRING_TOKEN_ALREADY_USED`.
 */
sealed interface PairingExchangeResponse {
    data class Success(val data: DeviceTokenResponseDto) : PairingExchangeResponse

    /** A non-2xx answer; [errorCode] is `null` when the body had none or could not be read. */
    data class HttpError(val code: Int, val errorCode: String?) : PairingExchangeResponse

    data class NetworkError(val cause: Throwable, val kind: NetworkErrorKind) : PairingExchangeResponse
}
