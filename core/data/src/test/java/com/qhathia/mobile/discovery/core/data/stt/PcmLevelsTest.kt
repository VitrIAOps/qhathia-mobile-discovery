package com.qhathia.mobile.discovery.core.data.stt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PcmLevelsTest {

    /** Encodes samples as 16-bit little-endian PCM. */
    private fun pcm(vararg samples: Int): ByteArray = ByteArray(samples.size * 2).also { bytes ->
        samples.forEachIndexed { i, sample ->
            bytes[i * 2] = (sample and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
    }

    @Test
    fun given_silence_buffer_then_peak_is_zero() {
        assertEquals(0, PcmLevels.peakAmplitude(pcm(0, 0, 0, 0)))
    }

    @Test
    fun given_speech_buffer_then_peak_is_largest_magnitude() {
        assertEquals(12_000, PcmLevels.peakAmplitude(pcm(300, -12_000, 8_000, 1_500)))
    }

    @Test
    fun given_full_scale_negative_sample_then_peak_is_clamped_to_short_max() {
        assertEquals(32_767, PcmLevels.peakAmplitude(pcm(-32_768)))
    }

    @Test
    fun given_length_shorter_than_buffer_then_only_the_read_bytes_count() {
        assertEquals(100, PcmLevels.peakAmplitude(pcm(100, 20_000), length = 2))
    }

    @Test
    fun given_trailing_odd_byte_then_it_is_ignored() {
        val buffer = pcm(50) + byteArrayOf(0x7F)

        assertEquals(50, PcmLevels.peakAmplitude(buffer))
    }
}
