package com.qhathia.mobile.discovery.core.domain

/**
 * Single source of truth for the voice-search timing budgets (business + technical rules), shared by
 * the presentation state machine, the recorder, and the network layer.
 */
object VoiceSearchConstants {
    /** Max wait for the consumer to provide audio input, in seconds (business rule). */
    const val INPUT_WAIT_SECONDS: Int = 30

    /** Maximum length of a single recording, in seconds (business rule). */
    const val MAX_RECORDING_SECONDS: Int = 45

    /**
     * Once speech has been detected, this much trailing silence ends the recording and triggers
     * transcription (end-of-speech detection / endpointing).
     */
    const val SILENCE_AUTO_STOP_MS: Long = 1_000

    /** How often the microphone level is sampled while recording. */
    const val LEVEL_POLL_INTERVAL_MS: Long = 200

    /** After this many seconds without a transcription response, show the slow-response message. */
    const val SLOW_RESPONSE_SECONDS: Int = 30

    /** An idle error screen returns to Home idle after this many seconds. */
    const val ERROR_IDLE_TIMEOUT_SECONDS: Int = 30

    /** Connection budget: token request plus the live-connection handshake (technical rule). */
    const val CONNECT_TIMEOUT_MS: Long = 10_000

    /** Budget for the final transcript after the end of the audio stream (technical rule). */
    const val RESPONSE_TIMEOUT_MS: Long = 120_000

    /** End-to-end operation budget enforced as an outer `withTimeout` (technical rule). */
    const val TOTAL_OPERATION_TIMEOUT_MS: Long = 150_000
}
