package com.qhathia.mobile.discovery.core.domain.audio

import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio

/** Classification of a single microphone level sample. */
enum class AudioLevel { Speech, Silence }

/**
 * Port over on-device microphone capture. Implemented in `core:data` with `MediaRecorder`.
 *
 * Lifecycle: [start] begins capture; [pollLevel] classifies the current input level and is called
 * periodically by the caller to drive end-of-speech detection and the no-audio guard; [stop] ends
 * capture and returns the clip (or `null` when nothing usable was captured); [cancel] aborts and
 * discards. The implementation enforces the maximum recording length internally.
 */
interface AudioRecorder {

    /** Begins capturing microphone audio. */
    suspend fun start()

    /** Samples the current microphone level once. Call periodically while recording. */
    fun pollLevel(): AudioLevel

    /** Stops capture and returns the recorded clip, or `null` if nothing usable was captured. */
    suspend fun stop(): RecordedAudio?

    /** Aborts an in-progress capture and discards any partial recording. */
    fun cancel()
}
