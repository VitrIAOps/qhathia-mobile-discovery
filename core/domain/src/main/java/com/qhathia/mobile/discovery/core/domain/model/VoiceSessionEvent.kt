package com.qhathia.mobile.discovery.core.domain.model

/**
 * Progress of one voice-search attempt, as emitted by
 * [com.qhathia.mobile.discovery.core.domain.usecase.VoiceSearchSession.events]. [Transcribed],
 * [NoAudio] and [Failed] are terminal: the flow completes right after emitting one of them.
 */
sealed interface VoiceSessionEvent {

    /**
     * Recording. [transcript] is the live text recognized so far (empty until the first words) and
     * [remainingSeconds] counts down from the recording cap.
     */
    data class Listening(val transcript: String, val remainingSeconds: Int) : VoiceSessionEvent

    /**
     * Recording ended; waiting for the final transcript. [transcript] is the live text recognized so
     * far and keeps updating, since results for the last words usually arrive after the recording ends.
     */
    data class Finalizing(val transcript: String) : VoiceSessionEvent

    /** The attempt produced [transcript], the search text. */
    data class Transcribed(val transcript: String) : VoiceSessionEvent

    /** The input-wait window elapsed without any detected speech. */
    data object NoAudio : VoiceSessionEvent

    /** The attempt failed; [category] selects the error copy. */
    data class Failed(val category: SttErrorCategory) : VoiceSessionEvent
}
