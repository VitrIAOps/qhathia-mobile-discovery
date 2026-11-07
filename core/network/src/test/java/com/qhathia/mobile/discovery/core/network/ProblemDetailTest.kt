package com.qhathia.mobile.discovery.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProblemDetailTest {

    private suspend fun call(status: HttpStatusCode, body: String, contentType: String = PROBLEM_JSON): NetworkResult<String> {
        val client = KtorClientFactory.create(
            MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, contentType)) },
        )
        return runCatching { client.get("https://api.test/x").bodyAsText() }.toNetworkResult()
    }

    @Test
    fun parses_price_changed_problem() = runTest {
        val body = """
            {"type":"/errors/price-changed","status":409,"errorCode":"PRICE_CHANGED",
             "detail":"El precio cambió","changes":[{"productId":"p1","productName":"QA X OK",
             "expectedUnitPrice":210.0,"currentUnitPrice":199.0}],"extra":"ignored"}
        """.trimIndent()

        val error = call(HttpStatusCode.Conflict, body) as NetworkResult.HttpError

        assertEquals(409, error.code)
        assertEquals("PRICE_CHANGED", error.errorCode)
        assertEquals("El precio cambió", error.problem?.detail)
        val change = error.problem!!.changes.single()
        assertEquals("p1", change.productId)
        assertEquals(199.0, change.currentUnitPrice)
    }

    @Test
    fun legacy_code_key_resolves_as_error_code() = runTest {
        val error = call(HttpStatusCode.Gone, """{"code":"PAIRING_TOKEN_EXPIRED"}""") as NetworkResult.HttpError

        assertEquals("PAIRING_TOKEN_EXPIRED", error.errorCode)
    }

    @Test
    fun empty_body_keeps_status_without_problem() = runTest {
        val error = call(HttpStatusCode.Unauthorized, "") as NetworkResult.HttpError

        assertEquals(401, error.code)
        assertNull(error.problem)
    }

    @Test
    fun html_body_keeps_status_without_problem() = runTest {
        val error = call(HttpStatusCode.BadGateway, "<html>502 Bad Gateway</html>", "text/html") as NetworkResult.HttpError

        assertEquals(502, error.code)
        assertNull(error.problem)
    }

    @Test
    fun malformed_json_keeps_status_without_problem() = runTest {
        val error = call(HttpStatusCode.Conflict, """{"errorCode":""") as NetworkResult.HttpError

        assertEquals(409, error.code)
        assertNull(error.problem)
    }

    @Test
    fun success_is_untouched() = runTest {
        val result = call(HttpStatusCode.OK, "ok", "text/plain")

        assertEquals("ok", (result as NetworkResult.Success).data)
    }

    private companion object {
        const val PROBLEM_JSON = "application/problem+json"
    }
}
