package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.TranscriptUpdate
import com.qhathia.mobile.discovery.core.domain.repository.LiveTranscriptionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Scripted [LiveTranscriptionRepository] for tests. Emits [partials] right away, then (unless
 * [endEarly]) waits for the audio stream to complete, emits [latePartials] (results that arrive
 * after the recording ended), waits [finalDelayMillis] and emits `Final(finalText)`. [failure] is
 * thrown right after the partials (a dropped stream); [finalFailure] is thrown instead of the final.
 */
class FakeLiveTranscriptionRepository(
    var partials: List<String> = listOf("taladro", "taladro de la marca"),
    var latePartials: List<String> = emptyList(),
    var finalText: String = "taladro de la marca bosch",
    var failure: SttException? = null,
    var finalFailure: SttException? = null,
    var finalDelayMillis: Long = 0,
    var endEarly: Boolean = false,
) : LiveTranscriptionRepository {

    var calls: Int = 0
        private set
    var receivedChunks: Int = 0
        private set

    override fun transcribe(audio: Flow<ByteArray>): Flow<TranscriptUpdate> = flow {
        calls++
        partials.forEach { emit(TranscriptUpdate.Partial(it)) }
        failure?.let { throw it }
        if (!endEarly) audio.collect { receivedChunks++ }
        latePartials.forEach { emit(TranscriptUpdate.Partial(it)) }
        if (finalDelayMillis > 0) delay(finalDelayMillis)
        finalFailure?.let { throw it }
        emit(TranscriptUpdate.Final(finalText))
    }
}
