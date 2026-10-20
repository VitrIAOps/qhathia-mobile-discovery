package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceCommandAckDto
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceCommandApiServiceTest {

    private var captured: HttpRequestData? = null

    private fun service(status: HttpStatusCode = HttpStatusCode.NoContent): DeviceCommandApiService {
        val engine = MockEngine { request ->
            captured = request
            respond("", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return DeviceCommandApiService(KtorClientFactory.create(engine))
    }

    private fun sentJson(): JsonObject {
        val content = captured!!.body as OutgoingContent.ByteArrayContent
        return Json.parseToJsonElement(content.bytes().decodeToString()).jsonObject
    }

    @Test
    fun posts_ack_for_the_command_with_device_token() = runTest {
        val result = service().ack("device-jwt", "cmd-42", DeviceCommandAckDto(success = true))

        val request = captured!!
        assertTrue(result is NetworkResult.Success)
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.encodedPath.endsWith("/api/v1/devices/me/commands/cmd-42/ack"))
        assertEquals("Bearer device-jwt", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun omits_detail_when_null() = runTest {
        service().ack("t", "c1", DeviceCommandAckDto(success = true))

        val json = sentJson()
        assertEquals("true", json["success"]!!.jsonPrimitive.content)
        assertFalse(json.containsKey("detail"))
    }

    @Test
    fun sends_failure_detail() = runTest {
        service().ack("t", "c1", DeviceCommandAckDto(success = false, detail = "sin conexión"))

        val json = sentJson()
        assertEquals("false", json["success"]!!.jsonPrimitive.content)
        assertEquals("sin conexión", json["detail"]!!.jsonPrimitive.content)
    }

    @Test
    fun maps_404_to_http_error() = runTest {
        val result = service(HttpStatusCode.NotFound).ack("t", "c1", DeviceCommandAckDto(success = true))

        assertEquals(404, (result as NetworkResult.HttpError).code)
    }
}
