package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.SttException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * In-memory [AudioRecorder] for tests. [level] is returned by every [pollLevel] call and can be
 * flipped between time advances to drive end-of-speech detection. The chunk stream completes on
 * [stop] and fails on [cancel]; push audio with [emitChunk]. Set [startFailure] to make [start] fail.
 */
class FakeAudioRecorder(
    var level: AudioLevel = AudioLevel.Speech,
    var startFailure: SttException? = null,
) : AudioRecorder {

    private var chunks: Channel<ByteArray>? = null

    var isRecording: Boolean = false
        private set
    var stopped: Boolean = false
        private set
    var cancelled: Boolean = false
        private set

    override suspend fun start() {
        startFailure?.let { throw it }
        chunks = Channel(Channel.UNLIMITED)
        isRecording = true
        stopped = false
        cancelled = false
    }

    override fun audioChunks(): Flow<ByteArray> = chunks?.receiveAsFlow() ?: emptyFlow()

    override fun pollLevel(): AudioLevel = level

    override suspend fun stop() {
        if (!isRecording) return
        isRecording = false
        stopped = true
        chunks?.close()
    }

    override fun cancel() {
        if (!isRecording) return
        isRecording = false
        cancelled = true
        chunks?.cancel(CancellationException("Recording cancelled"))
    }

    /** Pushes one PCM chunk into the current session's stream. */
    fun emitChunk(chunk: ByteArray = ByteArray(CHUNK_BYTES)) {
        chunks?.trySend(chunk)
    }

    private companion object {
        const val CHUNK_BYTES = 3_200
    }
}
