package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.VoiceSearchResult
import com.qhathia.mobile.discovery.core.domain.repository.SpeechToTextRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Transcribes captured audio through the speech-to-text pipeline, enforcing the end-to-end
 * operation budget. Empty audio short-circuits to an [SttErrorCategory.EmptyAudio] failure before
 * any network call; exceeding the total budget maps to [SttErrorCategory.Timeout].
 */
class TranscribeVoiceSearchUseCase @Inject constructor(
    private val repository: SpeechToTextRepository,
) {
    suspend operator fun invoke(audio: RecordedAudio): Result<VoiceSearchResult> {
        if (audio.isEmpty) {
            return Result.failure(SttException(SttErrorCategory.EmptyAudio))
        }
        return try {
            withTimeout(VoiceSearchConstants.TOTAL_OPERATION_TIMEOUT_MS) {
                repository.transcribe(audio)
            }
        } catch (timeout: TimeoutCancellationException) {
            Result.failure(SttException(SttErrorCategory.Timeout))
        }
    }
}
