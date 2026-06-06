package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.Serializable

/** Request body for `POST /api/v1/auth/login`. */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/**
 * Login response. Only the access token is needed here; `ignoreUnknownKeys` drops the rest
 * (`refreshToken`, `user`, `store`, `stores`, …).
 */
@Serializable
data class LoginResponseDto(
    val accessToken: String,
)
