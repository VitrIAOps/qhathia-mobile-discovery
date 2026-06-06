package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeepgramAlternativeDto
import com.qhathia.mobile.discovery.core.network.dto.DeepgramChannelDto
import com.qhathia.mobile.discovery.core.network.dto.DeepgramListenResponseDto
import com.qhathia.mobile.discovery.core.network.dto.DeepgramResultsDto
import com.qhathia.mobile.discovery.core.network.dto.SttTokenResponseDto
import com.qhathia.mobile.discovery.core.network.service.DeepgramClient
import com.qhathia.mobile.discovery.core.network.service.SttTokenApiService
import com.qhathia.mobile.discovery.core.testing.VoiceSearchFixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class SpeechToTextRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>()
    private val sttTokenApiService = mockk<SttTokenApiService>()
    private val deepgramClient = mockk<DeepgramClient>()

    private fun repository() =
        SpeechToTextRepositoryImpl(sessionTokenProvider, sttTokenApiService, deepgramClient)

    private fun deepgramResponse(transcript: String, confidence: Double = 0.9) =
        DeepgramListenResponseDto(
            results = DeepgramResultsDto(
                channels = listOf(
                    DeepgramChannelDto(alternatives = listOf(DeepgramAlternativeDto(transcript, confidence))),
                ),
            ),
        )

    @Test
    fun given_token_and_transcript_when_transcribe_then_success() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns NetworkResult.Success(SttTokenResponseDto(token = "dg"))
        coEvery { deepgramClient.transcribe(any(), any(), "dg") } returns
            NetworkResult.Success(deepgramResponse("taladro bosch", 0.97))

        val result = repository().transcribe(VoiceSearchFixtures.recordedAudio())

        assertEquals("taladro bosch", result.getOrNull()?.transcript)
        assertEquals(0.97, result.getOrNull()?.confidence)
    }

    @Test
    fun given_blank_transcript_when_transcribe_then_empty_audio() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns NetworkResult.Success(SttTokenResponseDto(token = "dg"))
        coEvery { deepgramClient.transcribe(any(), any(), "dg") } returns
            NetworkResult.Success(deepgramResponse("   "))

        val result = repository().transcribe(VoiceSearchFixtures.recordedAudio())

        assertEquals(SttErrorCategory.EmptyAudio, (result.exceptionOrNull() as SttException).category)
    }

    @Test
    fun given_token_endpoint_server_error_when_transcribe_then_transcription_response() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns NetworkResult.HttpError(502, "Bad Gateway")

        val result = repository().transcribe(VoiceSearchFixtures.recordedAudio())

        assertEquals(SttErrorCategory.TranscriptionResponse, (result.exceptionOrNull() as SttException).category)
    }

    @Test
    fun given_deepgram_connectivity_failure_when_transcribe_then_network() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns NetworkResult.Success(SttTokenResponseDto(token = "dg"))
        coEvery { deepgramClient.transcribe(any(), any(), "dg") } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().transcribe(VoiceSearchFixtures.recordedAudio())

        assertEquals(SttErrorCategory.Network, (result.exceptionOrNull() as SttException).category)
    }

    @Test
    fun given_auth_failure_when_transcribe_then_propagates_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns
            Result.failure(SttException(SttErrorCategory.Network))

        val result = repository().transcribe(VoiceSearchFixtures.recordedAudio())

        assertTrue(result.isFailure)
        assertEquals(SttErrorCategory.Network, (result.exceptionOrNull() as SttException).category)
    }
}
