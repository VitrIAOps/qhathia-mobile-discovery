package com.qhathia.mobile.discovery.core.domain.model

/**
 * A successful transcription of a voice-search attempt.
 *
 * @property transcript the recognized text.
 * @property confidence the provider's confidence in `[0, 1]`.
 */
data class VoiceSearchResult(
    val transcript: String,
    val confidence: Double,
)
