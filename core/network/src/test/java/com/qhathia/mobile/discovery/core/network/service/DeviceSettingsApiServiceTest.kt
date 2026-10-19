package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceSettingsApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): DeviceSettingsApiService {
        val engine = MockEngine { request ->
            captured = request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return DeviceSettingsApiService(KtorClientFactory.create(engine))
    }

    @Test
    fun gets_own_settings_with_device_token() = runTest {
        service("{}").getMySettings("device-jwt")

        val request = captured!!
        assertEquals(HttpMethod.Get, request.method)
        assertTrue(request.url.encodedPath.endsWith("/api/v1/devices/me/settings"))
        assertEquals("Bearer device-jwt", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun parses_full_settings() = runTest {
        val body = """
            {"deviceId":"d1","idleShowcaseEnabled":false,"voiceAssistantEnabled":false,"brightnessMode":"HIGH",
             "kioskMode":false,"exitPinHash":"${'$'}2a${'$'}10${'$'}abc","autoSyncEnabled":false,"settingsVersion":7}
        """.trimIndent()

        val dto = (service(body).getMySettings("t") as NetworkResult.Success).data

        assertEquals(false, dto.voiceAssistantEnabled)
        assertEquals("HIGH", dto.brightnessMode)
        assertEquals(false, dto.kioskMode)
        assertEquals("\$2a\$10\$abc", dto.exitPinHash)
        assertEquals(7L, dto.settingsVersion)
    }

    @Test
    fun missing_fields_fall_back_to_server_defaults() = runTest {
        val dto = (service("{}").getMySettings("t") as NetworkResult.Success).data

        assertEquals(true, dto.voiceAssistantEnabled)
        assertEquals("AUTO", dto.brightnessMode)
        assertEquals(true, dto.kioskMode)
        assertNull(dto.exitPinHash)
        assertNull(dto.settingsVersion)
    }

    @Test
    fun maps_401_to_http_error() = runTest {
        val result = service("{}", HttpStatusCode.Unauthorized).getMySettings("t")

        assertEquals(401, (result as NetworkResult.HttpError).code)
    }
}
