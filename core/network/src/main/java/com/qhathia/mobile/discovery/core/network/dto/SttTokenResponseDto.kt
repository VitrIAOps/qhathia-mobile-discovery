package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Response of `POST /api/v1/stt/token` — a short-lived Deepgram credential (the example token
 * `dg_temp_key_...` is API-key shaped, hence the `Token` auth scheme on the Deepgram call).
 */
@Serializable
data class SttTokenResponseDto(
    val token: String,
    val expiresIn: Int = 0,
    val provider: String = "",
    val issuedAt: String = "",
)
