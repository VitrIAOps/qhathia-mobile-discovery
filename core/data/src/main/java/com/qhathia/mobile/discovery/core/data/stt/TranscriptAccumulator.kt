package com.qhathia.mobile.discovery.core.data.stt

/**
 * Builds the running transcript from Deepgram live results: final segments are confirmed and kept;
 * an interim segment is shown after them until the next result replaces it.
 */
internal class TranscriptAccumulator {

    private val confirmed = mutableListOf<String>()
    private var interim = ""

    /** The transcript so far (confirmed segments, then the current interim one). */
    val transcript: String
        get() = (confirmed + interim).filter { it.isNotEmpty() }.joinToString(" ")

    /** Applies one result; returns the new [transcript], or `null` when it did not change. */
    fun apply(text: String, isFinal: Boolean): String? {
        val before = transcript
        val segment = text.trim()
        if (isFinal) {
            if (segment.isNotEmpty()) confirmed += segment
            interim = ""
        } else {
            interim = segment
        }
        return transcript.takeIf { it != before }
    }
}
