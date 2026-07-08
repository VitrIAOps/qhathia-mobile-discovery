package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTokenProviderTest {

    @Test
    fun given_persisted_device_token_then_used() = runTest {
        val session = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "device-jwt"))
        val provider = SessionTokenProvider(session)

        val token = provider.accessToken()

        assertEquals("device-jwt", token.getOrNull())
    }

    @Test
    fun given_no_device_token_then_fails_without_login() = runTest {
        val session = FakeStoreSessionRepository()
        val provider = SessionTokenProvider(session)

        val token = provider.accessToken()

        assertTrue(token.isFailure)
    }
}
