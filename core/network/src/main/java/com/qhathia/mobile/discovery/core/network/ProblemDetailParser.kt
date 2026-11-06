package com.qhathia.mobile.discovery.core.network

import com.qhathia.mobile.discovery.core.network.dto.ProblemDetailDto
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

private val problemJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/**
 * Reads the `problem+json` body of an error response. Returns `null` when the body is empty, is not
 * JSON, or cannot be read; never throws (except cancellation).
 */
suspend fun HttpResponse.readProblemDetail(): ProblemDetailDto? = try {
    val text = bodyAsText()
    if (text.isBlank()) null else problemJson.decodeFromString(ProblemDetailDto.serializer(), text)
} catch (cancel: CancellationException) {
    throw cancel
} catch (_: Exception) {
    null
}

/** [readProblemDetail] of the failed response. */
suspend fun ResponseException.readProblemDetail(): ProblemDetailDto? = response.readProblemDetail()
