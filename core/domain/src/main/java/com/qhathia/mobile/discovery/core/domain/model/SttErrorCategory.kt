package com.qhathia.mobile.discovery.core.domain.model

/**
 * The classes of failure a voice-search attempt can end in. The presentation layer maps each to its
 * own error title/message; the data layer maps causes (network, HTTP status, blank transcript, …)
 * to one of these.
 */
enum class SttErrorCategory {
    /** Lost or unstable network connectivity. */
    Network,

    /** Captured audio exceeded the accepted length for transcription. */
    AudioTooLong,

    /** The provider returned a transcription failure or a malformed response. */
    TranscriptionResponse,

    /** Captured audio contained no detectable speech. */
    EmptyAudio,

    /** A per-phase or total-operation budget was exceeded. */
    Timeout,

    /** Any failure that does not match a more specific category. */
    Generic,
}
