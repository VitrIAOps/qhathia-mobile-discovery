package com.qhathia.mobile.discovery.core.voice.transcription

import kotlinx.coroutines.flow.Flow

/** One recognized segment: interim until [isFinal]. */
data class TranscriptResult(val text: String, val isFinal: Boolean)

/**
 * Streams raw PCM audio (linear16, 16 kHz, mono) to a speech provider. The flow completes when the
 * audio ends and the provider has flushed its last results; any other ending fails it with a
 * [VoiceStreamException]. [token] is a credential obtained by the host app and is never logged.
 */
fun interface LiveTranscriber {
    fun stream(audio: Flow<ByteArray>, token: String): Flow<TranscriptResult>
}

/** How the connection to the speech provider failed at transport level. */
enum class VoiceTransportFailure { Timeout, Connectivity, Unknown }

/**
 * Why a live transcription stream failed. Exactly one of [closeCode] (the provider closed the socket
 * with a non-normal code), [handshakeRejected] (the upgrade was refused) or [transport] describes it.
 */
class VoiceStreamException(
    val closeCode: Int? = null,
    val handshakeRejected: Boolean = false,
    val transport: VoiceTransportFailure? = null,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: "Live transcription stream failed", cause)
