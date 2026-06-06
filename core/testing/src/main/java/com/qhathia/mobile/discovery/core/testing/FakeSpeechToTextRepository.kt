package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio
import com.qhathia.mobile.discovery.core.domain.model.VoiceSearchResult
import com.qhathia.mobile.discovery.core.domain.repository.SpeechToTextRepository
import kotlinx.coroutines.delay

/**
 * Configurable [SpeechToTextRepository] for tests. [delayMillis] simulates a slow transcription so
 * total-operation timeouts and slow-response messaging can be exercised on virtual time.
 */
class FakeSpeechToTextRepository(
    var result: Result<VoiceSearchResult> = Result.success(VoiceSearchFixtures.result()),
    var delayMillis: Long = 0,
) : SpeechToTextRepository {

    var lastAudio: RecordedAudio? = null
        private set

    override suspend fun transcribe(audio: RecordedAudio): Result<VoiceSearchResult> {
        lastAudio = audio
        if (delayMillis > 0) delay(delayMillis)
        return result
    }
}
