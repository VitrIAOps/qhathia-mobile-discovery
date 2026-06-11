package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.LoginResponseDto
import com.qhathia.mobile.discovery.core.network.service.AuthApiService
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTokenProviderTest {

    private val authApiService = mockk<AuthApiService>()

    @Test
    fun given_persisted_device_token_then_used_without_login() = runTest {
        val session = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "device-jwt"))
        val provider = SessionTokenProvider(authApiService, session)

        val token = provider.accessToken()

        assertEquals("device-jwt", token.getOrNull())
        coVerify(exactly = 0) { authApiService.login(any(), any()) }
    }

    @Test
    fun given_no_device_token_then_falls_back_to_dev_login() = runTest {
        val session = FakeStoreSessionRepository()
        coEvery { authApiService.login(any(), any()) } returns
            NetworkResult.Success(LoginResponseDto(accessToken = "login-jwt"))
        val provider = SessionTokenProvider(authApiService, session)

        val token = provider.accessToken()

        assertEquals("login-jwt", token.getOrNull())
        coVerify(exactly = 1) { authApiService.login(any(), any()) }
    }
}
