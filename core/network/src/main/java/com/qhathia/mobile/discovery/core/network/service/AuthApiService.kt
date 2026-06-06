package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.LoginRequestDto
import com.qhathia.mobile.discovery.core.network.dto.LoginResponseDto
import com.qhathia.mobile.discovery.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

/** Calls `POST /api/v1/auth/login` to obtain the Qhathia session access token. */
class AuthApiService @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun login(email: String, password: String): NetworkResult<LoginResponseDto> =
        runCatching {
            client.post("${BuildConfig.QHATHIA_BASE_URL}/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(email = email, password = password))
            }.body<LoginResponseDto>()
        }.toNetworkResult()
}
