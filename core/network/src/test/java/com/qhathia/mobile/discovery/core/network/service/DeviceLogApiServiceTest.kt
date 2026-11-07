package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogEntryDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogsRequestDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceLogApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(status: HttpStatusCode = HttpStatusCode.NoContent): DeviceLogApiService {
        val engine = MockEngine { request ->
            captured = request
            respond("", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return DeviceLogApiService(KtorClientFactory.create(engine))
    }

    private fun sentJson(): JsonObject {
        val content = captured!!.body as OutgoingContent.ByteArrayContent
        return Json.parseToJsonElement(content.bytes().decodeToString()).jsonObject
    }

    private val lockout = DeviceLogEntryDto(
        level = "WARN",
        code = "SETTINGS_PIN_LOCKOUT",
        message = "Settings PIN locked after 5 wrong attempts",
        timestamp = "2026-11-06T12:00:00Z",
        context = mapOf("failedAttempts" to 5L, "lockoutSeconds" to 30L),
    )

    @Test
    fun posts_to_logs_path_with_device_bearer_token() = runTest {
        val result = service().send("device-jwt", DeviceLogsRequestDto(listOf(lockout)))

        assertEquals(NetworkResult.Success(Unit), result)
        val request = captured!!
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.encodedPath.endsWith("/api/v1/devices/logs"))
        assertEquals("Bearer device-jwt", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun sends_the_entry_with_its_context() = runTest {
        service().send("device-jwt", DeviceLogsRequestDto(listOf(lockout)))

        val entry = sentJson()["logs"]!!.jsonArray.single().jsonObject
        assertEquals(JsonPrimitive("WARN"), entry["level"])
        assertEquals(JsonPrimitive("SETTINGS_PIN_LOCKOUT"), entry["code"])
        assertEquals(JsonPrimitive("2026-11-06T12:00:00Z"), entry["timestamp"])
        assertEquals(JsonPrimitive(5), entry["context"]!!.jsonObject["failedAttempts"])
        assertEquals(JsonPrimitive(30), entry["context"]!!.jsonObject["lockoutSeconds"])
    }

    @Test
    fun omits_optional_fields_when_unknown() = runTest {
        service().send("t", DeviceLogsRequestDto(listOf(lockout.copy(code = null, context = null))))

        val entry = sentJson()["logs"]!!.jsonArray.single().jsonObject
        assertFalse(entry.containsKey("code"))
        assertFalse(entry.containsKey("context"))
    }

    @Test
    fun maps_401_to_http_error() = runTest {
        val result = service(HttpStatusCode.Unauthorized).send("t", DeviceLogsRequestDto(listOf(lockout)))

        assertEquals(401, (result as NetworkResult.HttpError).code)
    }
}
