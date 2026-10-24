package com.qhathia.mobile.discovery.core.domain.model

/** One update from a live transcription stream. */
sealed interface TranscriptUpdate {

    /** The transcript so far: confirmed segments followed by the current interim segment. */
    data class Partial(val text: String) : TranscriptUpdate

    /** The final transcript, emitted once when the stream ends normally. */
    data class Final(val text: String) : TranscriptUpdate
}
