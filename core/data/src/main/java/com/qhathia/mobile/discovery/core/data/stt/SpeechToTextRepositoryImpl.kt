package com.qhathia.mobile.discovery.core.data.stt

import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.VoiceSearchResult
import com.qhathia.mobile.discovery.core.domain.repository.SpeechToTextRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.DeepgramClient
import com.qhathia.mobile.discovery.core.network.service.SttTokenApiService
import javax.inject.Inject

/**
 * Orchestrates the post-recording pipeline: ensure a Qhathia JWT → fetch an ephemeral Deepgram token
 * → call Deepgram → map the response/cause to a [VoiceSearchResult] or an [SttException] tagged with
 * an [SttErrorCategory]. The ephemeral token is used once and not retained.
 */
class SpeechToTextRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val sttTokenApiService: SttTokenApiService,
    private val deepgramClient: DeepgramClient,
) : SpeechToTextRepository {

    override suspend fun transcribe(audio: RecordedAudio): Result<VoiceSearchResult> {
        logQuery(audio)
        if (audio.isEmpty) return failure(SttErrorCategory.EmptyAudio)

        val accessToken = sessionTokenProvider.accessToken().getOrElse { return logged(Result.failure(it)) }

        val deepgramToken = when (val tokenResult = sttTokenApiService.getToken(accessToken)) {
            is NetworkResult.Success -> tokenResult.data.token
            is NetworkResult.HttpError -> return failure(SttErrorMapper.fromTokenHttpError(tokenResult.code))
            is NetworkResult.NetworkError -> return failure(SttErrorMapper.fromNetworkError(tokenResult.kind))
        }

        return when (val transcription = deepgramClient.transcribe(audio.bytes, audio.mimeType, deepgramToken)) {
            is NetworkResult.Success -> {
                val alternative = transcription.data.results
                    ?.channels?.firstOrNull()
                    ?.alternatives?.firstOrNull()
                val transcript = alternative?.transcript?.trim().orEmpty()
                if (transcript.isEmpty()) {
                    failure(SttErrorCategory.EmptyAudio)
                } else {
                    logged(Result.success(VoiceSearchResult(transcript, alternative?.confidence ?: 0.0)))
                }
            }
            is NetworkResult.HttpError -> failure(SttErrorMapper.fromDeepgramHttpError(transcription.code))
            is NetworkResult.NetworkError -> failure(SttErrorMapper.fromNetworkError(transcription.kind))
        }
    }

    private fun failure(category: SttErrorCategory): Result<VoiceSearchResult> =
        logged(Result.failure(SttException(category)))

    private fun logged(result: Result<VoiceSearchResult>): Result<VoiceSearchResult> {
        if (BuildConfig.DEBUG) {
            result.fold(
                onSuccess = { Log.d(TAG, "STT result: transcript='${it.transcript}' confidence=${it.confidence}") },
                onFailure = { Log.d(TAG, "STT result: failure=${(it as? SttException)?.category ?: it.message}") },
            )
        }
        return result
    }

    private fun logQuery(audio: RecordedAudio) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "STT query: mime=${audio.mimeType} duration=${audio.durationMillis}ms size=${audio.bytes.size}B")
        }
    }

    private companion object {
        const val TAG = "VoiceSearchStt"
    }
}
