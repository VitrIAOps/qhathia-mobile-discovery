package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.testing.FakeSpeechToTextRepository
import com.qhathia.mobile.discovery.core.testing.VoiceSearchFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranscribeVoiceSearchUseCaseTest {

    @Test
    fun given_audio_when_transcribed_then_returns_result() = runTest {
        val repository = FakeSpeechToTextRepository(result = Result.success(VoiceSearchFixtures.result()))
        val useCase = TranscribeVoiceSearchUseCase(repository)

        val result = useCase(VoiceSearchFixtures.recordedAudio())

        assertEquals(VoiceSearchFixtures.result(), result.getOrNull())
    }

    @Test
    fun given_empty_audio_when_transcribed_then_fails_with_empty_audio_without_calling_repo() = runTest {
        val repository = FakeSpeechToTextRepository()
        val useCase = TranscribeVoiceSearchUseCase(repository)

        val result = useCase(VoiceSearchFixtures.emptyAudio())

        assertEquals(SttErrorCategory.EmptyAudio, (result.exceptionOrNull() as SttException).category)
        assertEquals(null, repository.lastAudio) // short-circuited before any network call
    }

    @Test
    fun given_transcription_exceeds_total_budget_then_fails_with_timeout() = runTest {
        val repository = FakeSpeechToTextRepository(delayMillis = 200_000) // > 150s total budget
        val useCase = TranscribeVoiceSearchUseCase(repository)

        val result = useCase(VoiceSearchFixtures.recordedAudio())

        assertTrue(result.isFailure)
        assertEquals(SttErrorCategory.Timeout, (result.exceptionOrNull() as SttException).category)
    }
}
