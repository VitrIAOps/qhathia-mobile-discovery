package com.qhathia.mobile.discovery.feature.home.model

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory

/**
 * State of the voice-search (microphone) interaction. The consumer is considered to be in an active
 * attempt while [Listening], [Transcribing], or [SlowResponse]; [Idle], [NoAudioRetry], [Success],
 * and [Error] are resting states.
 */
sealed interface MicState {

    /** No microphone interaction — the Home controls are visible. */
    data object Idle : MicState

    /**
     * Actively recording the consumer's audio. [remainingSeconds] counts down internally from the
     * recording cap; the UI shows the animated "Escuchando…" label rather than the number.
     */
    data class Listening(val remainingSeconds: Int) : MicState

    /** Audio captured; uploading and awaiting the transcript ("Organizando ideas…"). */
    data object Transcribing : MicState

    /** The transcript response is taking unusually long ("Está tardando más de lo que esperaba…"). */
    data object SlowResponse : MicState

    /** Transcription succeeded; [transcript] is the recognized search text. */
    data class Success(val transcript: String) : MicState

    /** Transcription failed; [category] selects the error screen copy. */
    data class Error(val category: SttErrorCategory) : MicState

    /**
     * A voice-search attempt ended with no detected audio. [promptIndex] is a monotonically
     * increasing index used to pick (and vary) the retry message from the available prompt set.
     */
    data class NoAudioRetry(val promptIndex: Int) : MicState
}
