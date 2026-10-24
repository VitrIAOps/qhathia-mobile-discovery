package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A `Results` message from Deepgram's live `/v1/listen` WebSocket. Only the first alternative's
 * transcript/confidence and the finality flags are read; `ignoreUnknownKeys` drops the rest. Other
 * message types (`Metadata`, `SpeechStarted`, `UtteranceEnd`) are filtered out before decoding.
 *
 * [isFinal] marks a segment that will not be revised; while `false` the segment is interim and the
 * next message replaces it. [speechFinal] marks Deepgram's own end-of-utterance (not used for
 * endpointing — the app detects end of speech on-device).
 */
@Serializable
data class DeepgramLiveResultDto(
    val type: String = "",
    @SerialName("is_final") val isFinal: Boolean = false,
    @SerialName("speech_final") val speechFinal: Boolean = false,
    val channel: DeepgramLiveChannelDto = DeepgramLiveChannelDto(),
) {
    /** The first alternative's transcript, or empty when Deepgram sent none. */
    val transcript: String get() = channel.alternatives.firstOrNull()?.transcript.orEmpty()
}

@Serializable
data class DeepgramLiveChannelDto(
    val alternatives: List<DeepgramLiveAlternativeDto> = emptyList(),
)

@Serializable
data class DeepgramLiveAlternativeDto(
    val transcript: String = "",
    val confidence: Double = 0.0,
)
