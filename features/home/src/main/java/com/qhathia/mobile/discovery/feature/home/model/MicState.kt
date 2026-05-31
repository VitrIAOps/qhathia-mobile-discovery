package com.qhathia.mobile.discovery.feature.home.model

/**
 * State of the microphone (voice search) interaction. The consumer is considered to be
 * "interacting with the mic" in any state other than [Idle].
 */
sealed interface MicState {

    /** No microphone interaction — the Home controls are visible. */
    data object Idle : MicState

    /**
     * Actively listening for audio. [remainingSeconds] counts down from the 30s business cap.
     */
    data class Listening(val remainingSeconds: Int) : MicState

    /**
     * A listening window ended without detected audio. [promptIndex] is a monotonically increasing
     * index used to pick (and vary) the retry message from the available prompt set.
     */
    data class NoAudioRetry(val promptIndex: Int) : MicState
}
