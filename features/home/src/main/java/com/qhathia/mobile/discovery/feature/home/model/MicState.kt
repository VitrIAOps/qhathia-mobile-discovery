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
     * recording cap (not shown). [liveTranscript] is the text recognized so far: while blank the UI
     * shows the animated "Escuchando…" label, then the live text replaces it.
     */
    data class Listening(val remainingSeconds: Int, val liveTranscript: String = "") : MicState

    /**
     * Recording ended; awaiting the final transcript. [liveTranscript] is the text heard so far (it keeps
     * updating as late results arrive) and replaces "Organizando ideas…", which is shown only while blank.
     */
    data class Transcribing(val liveTranscript: String = "") : MicState

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
