package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.TranscriptUpdate
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeepgramLiveAlternativeDto
import com.qhathia.mobile.discovery.core.network.dto.DeepgramLiveChannelDto
import com.qhathia.mobile.discovery.core.network.dto.DeepgramLiveResultDto
import com.qhathia.mobile.discovery.core.network.dto.SttTokenResponseDto
import com.qhathia.mobile.discovery.core.network.service.DeepgramLiveClient
import com.qhathia.mobile.discovery.core.network.service.DeepgramStreamException
import com.qhathia.mobile.discovery.core.network.service.SttTokenApiService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.UnknownHostException

class LiveTranscriptionRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>()
    private val sttTokenApiService = mockk<SttTokenApiService>()
    private val deepgramLiveClient = mockk<DeepgramLiveClient>()
    private val audio: Flow<ByteArray> = emptyFlow()

    private fun repository() =
        LiveTranscriptionRepositoryImpl(sessionTokenProvider, sttTokenApiService, deepgramLiveClient)

    private fun result(transcript: String, isFinal: Boolean) = DeepgramLiveResultDto(
        type = "Results",
        isFinal = isFinal,
        channel = DeepgramLiveChannelDto(listOf(DeepgramLiveAlternativeDto(transcript, 0.9))),
    )

    private fun givenToken() {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns NetworkResult.Success(SttTokenResponseDto(token = "dg"))
    }

    private suspend fun failureOf(block: suspend () -> Unit): SttErrorCategory =
        assertThrows<SttException> { block() }.category

    @Test
    fun given_stream_results_then_partials_then_final_transcript() = runTest {
        givenToken()
        every { deepgramLiveClient.stream(any(), "dg") } returns flowOf(
            result("taladro", isFinal = false),
            result("taladro de la marca", isFinal = true),
            result("bosch", isFinal = false),
            result("bosch", isFinal = true),
        )

        val updates = repository().transcribe(audio).toList()

        assertEquals(
            listOf(
                TranscriptUpdate.Partial("taladro"),
                TranscriptUpdate.Partial("taladro de la marca"),
                TranscriptUpdate.Partial("taladro de la marca bosch"),
                TranscriptUpdate.Final("taladro de la marca bosch"),
            ),
            updates,
        )
    }

    @Test
    fun given_blank_final_transcript_then_empty_audio() = runTest {
        givenToken()
        every { deepgramLiveClient.stream(any(), "dg") } returns flowOf(result("  ", isFinal = true))

        assertEquals(SttErrorCategory.EmptyAudio, failureOf { repository().transcribe(audio).toList() })
    }

    @Test
    fun given_token_server_error_then_transcription_response_and_no_stream() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns NetworkResult.HttpError(503, "Unavailable")

        assertEquals(SttErrorCategory.TranscriptionResponse, failureOf { repository().transcribe(audio).toList() })
        verify(exactly = 0) { deepgramLiveClient.stream(any(), any()) }
    }

    @Test
    fun given_token_network_failure_then_network() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { sttTokenApiService.getToken("jwt") } returns
            NetworkResult.NetworkError(UnknownHostException(), NetworkErrorKind.Connectivity)

        assertEquals(SttErrorCategory.Network, failureOf { repository().transcribe(audio).toList() })
    }

    @Test
    fun given_device_not_paired_then_generic() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns
            Result.failure(SttException(SttErrorCategory.Generic))

        assertEquals(SttErrorCategory.Generic, failureOf { repository().transcribe(audio).toList() })
    }

    @Test
    fun given_socket_drops_mid_stream_then_partials_emitted_then_network() = runTest {
        givenToken()
        every { deepgramLiveClient.stream(any(), "dg") } returns flow {
            emit(result("taladro", isFinal = false))
            throw DeepgramStreamException(kind = NetworkErrorKind.Connectivity)
        }
        val updates = mutableListOf<TranscriptUpdate>()

        val category = failureOf { repository().transcribe(audio).collect { updates += it } }

        assertEquals(SttErrorCategory.Network, category)
        assertEquals(listOf(TranscriptUpdate.Partial("taladro")), updates)
    }

    @Test
    fun given_handshake_rejected_then_transcription_response() = runTest {
        givenToken()
        every { deepgramLiveClient.stream(any(), "dg") } returns flow {
            throw DeepgramStreamException(handshakeRejected = true)
        }

        assertEquals(SttErrorCategory.TranscriptionResponse, failureOf { repository().transcribe(audio).toList() })
    }

    @Test
    fun given_stream_failures_then_mapper_classifies_close_codes_and_transport() {
        assertEquals(SttErrorCategory.TranscriptionResponse, SttErrorMapper.fromStreamFailure(1008, false, null))
        assertEquals(SttErrorCategory.TranscriptionResponse, SttErrorMapper.fromStreamFailure(4001, false, null))
        assertEquals(SttErrorCategory.Network, SttErrorMapper.fromStreamFailure(1011, false, null))
        assertEquals(SttErrorCategory.Timeout, SttErrorMapper.fromStreamFailure(null, false, NetworkErrorKind.Timeout))
        assertTrue(SttErrorMapper.fromStreamFailure(null, false, null) == SttErrorCategory.Generic)
    }
}
