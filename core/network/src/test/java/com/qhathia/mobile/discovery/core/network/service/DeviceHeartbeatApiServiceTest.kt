package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatRequestDto
import com.qhathia.mobile.discovery.core.network.dto.PendingCommandDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceHeartbeatApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"received":true,"serverTime":"2026-10-18T20:05:00Z"}""",
    ): DeviceHeartbeatApiService {
        val engine = MockEngine { request -> captured = request; reply(status, body) }
        return DeviceHeartbeatApiService(KtorClientFactory.create(engine))
    }

    private fun MockRequestHandleScope.reply(status: HttpStatusCode, body: String): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun sentJson(): JsonObject {
        val content = captured!!.body as OutgoingContent.ByteArrayContent
        return Json.parseToJsonElement(content.bytes().decodeToString()).jsonObject
    }

    @Test
    fun posts_to_heartbeat_path_with_device_bearer_token() = runTest {
        service().sendHeartbeat("device-jwt", HeartbeatRequestDto(appVersion = "1.4.0"))

        val request = captured!!
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.encodedPath.endsWith("/api/v1/devices/heartbeat"))
        assertEquals("Bearer device-jwt", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun sends_telemetry_and_omits_unknown_fields() = runTest {
        service().sendHeartbeat(
            "device-jwt",
            HeartbeatRequestDto(appVersion = "1.4.0", batteryLevel = 85, networkStatus = "WIFI"),
        )

        val json = sentJson()
        assertEquals("1.4.0", json["appVersion"]!!.jsonPrimitive.content)
        assertEquals("85", json["batteryLevel"]!!.jsonPrimitive.content)
        assertEquals("WIFI", json["networkStatus"]!!.jsonPrimitive.content)
        assertFalse(json.containsKey("model"))
        assertFalse(json.containsKey("uptimeSeconds"))
    }

    @Test
    fun parses_full_response() = runTest {
        val body = """
            {"received":true,"serverTime":"2026-10-18T20:05:00Z","catalogVersion":1713200000000,
             "settingsVersion":4,"pendingCommands":[{"id":"c1","command":"REFRESH_CATALOG"}],
             "somethingNew":"ignored"}
        """.trimIndent()

        val result = service(body = body).sendHeartbeat("t", HeartbeatRequestDto(appVersion = "1"))

        val dto = (result as NetworkResult.Success).data
        assertEquals(4L, dto.settingsVersion)
        assertEquals(1_713_200_000_000L, dto.catalogVersion)
        assertEquals(listOf(PendingCommandDto("c1", "REFRESH_CATALOG")), dto.pendingCommands)
    }

    @Test
    fun parses_minimal_response_from_older_backend() = runTest {
        val result = service().sendHeartbeat("t", HeartbeatRequestDto(appVersion = "1"))

        val dto = (result as NetworkResult.Success).data
        assertEquals(null, dto.settingsVersion)
        assertEquals(null, dto.catalogVersion)
        assertTrue(dto.pendingCommands.isEmpty())
    }

    @Test
    fun maps_401_to_http_error() = runTest {
        val result = service(HttpStatusCode.Unauthorized, "{}").sendHeartbeat("t", HeartbeatRequestDto(appVersion = "1"))

        assertEquals(401, (result as NetworkResult.HttpError).code)
    }

    @Test
    fun maps_500_to_http_error() = runTest {
        val result = service(HttpStatusCode.InternalServerError, "{}")
            .sendHeartbeat("t", HeartbeatRequestDto(appVersion = "1"))

        assertEquals(500, (result as NetworkResult.HttpError).code)
    }
}
