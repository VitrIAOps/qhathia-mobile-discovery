package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.TranscriptUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Port for live speech-to-text. Implemented in `core:data`, which obtains a Deepgram token from the
 * Qhathia backend and streams [audio] to Deepgram Live.
 */
interface LiveTranscriptionRepository {

    /**
     * Streams [audio] (raw PCM chunks) for transcription. Emits [TranscriptUpdate.Partial] while the
     * consumer speaks and one [TranscriptUpdate.Final] after [audio] completes, then completes. Fails
     * with an [com.qhathia.mobile.discovery.core.domain.model.SttException] tagged with a category
     * (a blank final transcript fails with `EmptyAudio`).
     */
    fun transcribe(audio: Flow<ByteArray>): Flow<TranscriptUpdate>
}
