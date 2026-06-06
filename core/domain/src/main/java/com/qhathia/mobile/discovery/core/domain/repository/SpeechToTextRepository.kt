package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio
import com.qhathia.mobile.discovery.core.domain.model.VoiceSearchResult

/**
 * Port for transcribing captured audio. Implemented in `core:data`, which obtains a Deepgram token
 * from the Qhathia backend and calls the provider, mapping every failure to an
 * [com.qhathia.mobile.discovery.core.domain.model.SttException] carrying an
 * [com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory].
 */
interface SpeechToTextRepository {

    /** Transcribes [audio], returning the recognized text or a failed [Result] tagged with a category. */
    suspend fun transcribe(audio: RecordedAudio): Result<VoiceSearchResult>
}
