package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio
import com.qhathia.mobile.discovery.core.domain.model.VoiceSearchResult

/** Canonical voice-search test data so tests don't re-declare audio/result shapes. */
object VoiceSearchFixtures {

    fun recordedAudio(
        bytes: ByteArray = byteArrayOf(1, 2, 3, 4),
        mimeType: String = "audio/ogg",
        durationMillis: Long = 2_000,
    ): RecordedAudio = RecordedAudio(bytes = bytes, mimeType = mimeType, durationMillis = durationMillis)

    fun emptyAudio(): RecordedAudio =
        RecordedAudio(bytes = ByteArray(0), mimeType = "audio/ogg", durationMillis = 0)

    fun result(
        transcript: String = "taladro de la marca bosch",
        confidence: Double = 0.96,
    ): VoiceSearchResult = VoiceSearchResult(transcript = transcript, confidence = confidence)
}
