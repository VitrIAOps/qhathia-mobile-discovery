package com.qhathia.mobile.discovery.core.domain.audio

import kotlinx.coroutines.flow.Flow

/** Classification of a single microphone level sample. */
enum class AudioLevel { Speech, Silence }

/**
 * Port over on-device microphone capture. Implemented in `core:data` with `AudioRecord`, producing
 * raw PCM (16-bit little-endian, 16 kHz, mono) for live streaming.
 *
 * Lifecycle: [start] begins capture; [audioChunks] streams the captured PCM of the current session
 * (call it once, after [start]) and completes when [stop] is called; [pollLevel] classifies the input
 * level since the previous call and is polled periodically to drive end-of-speech detection and the
 * no-audio guard; [stop] ends capture normally; [cancel] aborts capture and fails the chunk stream.
 */
interface AudioRecorder {

    /** Begins capturing microphone audio. Fails with an `SttException` if the microphone can't start. */
    suspend fun start()

    /** The PCM chunks of the current session. Audio captured before collection starts is buffered. */
    fun audioChunks(): Flow<ByteArray>

    /** Samples the input level since the previous call. Call periodically while recording. */
    fun pollLevel(): AudioLevel

    /** Stops capture; the chunk stream completes after the last buffered chunk. */
    suspend fun stop()

    /** Aborts an in-progress capture and discards any buffered audio. */
    fun cancel()
}
