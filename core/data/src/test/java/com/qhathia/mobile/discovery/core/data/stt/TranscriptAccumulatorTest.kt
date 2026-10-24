package com.qhathia.mobile.discovery.core.data.stt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TranscriptAccumulatorTest {

    private val accumulator = TranscriptAccumulator()

    @Test
    fun given_interim_results_then_each_replaces_the_previous_interim() {
        assertEquals("tala", accumulator.apply("tala", isFinal = false))
        assertEquals("taladro de", accumulator.apply("taladro de", isFinal = false))
    }

    @Test
    fun given_final_then_interim_then_interim_is_shown_after_confirmed_text() {
        accumulator.apply("taladro", isFinal = true)

        assertEquals("taladro de la", accumulator.apply("de la", isFinal = false))
    }

    @Test
    fun given_final_segment_replacing_interim_then_transcript_keeps_confirmed_text() {
        accumulator.apply("taladro de la marca", isFinal = false)
        accumulator.apply("taladro de la marca bosch", isFinal = true)

        assertEquals("taladro de la marca bosch", accumulator.transcript)
    }

    @Test
    fun given_unchanged_result_then_returns_null() {
        accumulator.apply("taladro", isFinal = false)

        assertNull(accumulator.apply("taladro ", isFinal = false))
    }

    @Test
    fun given_blank_final_segment_then_it_clears_the_interim_and_is_not_kept() {
        accumulator.apply("uh", isFinal = false)
        accumulator.apply("  ", isFinal = true)

        assertEquals("", accumulator.transcript)
    }
}
