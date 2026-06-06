package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subset of Deepgram's pre-recorded `/v1/listen` response. Only the first channel's first
 * alternative transcript/confidence and a little metadata are read; `ignoreUnknownKeys` drops the
 * rest (words, paragraphs, intelligence features, …).
 */
@Serializable
data class DeepgramListenResponseDto(
    val metadata: DeepgramMetadataDto? = null,
    val results: DeepgramResultsDto? = null,
)

@Serializable
data class DeepgramMetadataDto(
    @SerialName("request_id") val requestId: String = "",
    val duration: Double = 0.0,
)

@Serializable
data class DeepgramResultsDto(
    val channels: List<DeepgramChannelDto> = emptyList(),
)

@Serializable
data class DeepgramChannelDto(
    val alternatives: List<DeepgramAlternativeDto> = emptyList(),
)

@Serializable
data class DeepgramAlternativeDto(
    val transcript: String = "",
    val confidence: Double = 0.0,
)
