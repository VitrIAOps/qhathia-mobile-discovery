package com.qhathia.mobile.discovery.core.data.stt

import kotlin.math.abs

/** Level helpers for raw 16-bit little-endian PCM. */
internal object PcmLevels {

    /**
     * Peak absolute sample in the first [length] bytes of [buffer] (16-bit little-endian), on the
     * same 0–32767 scale `MediaRecorder.getMaxAmplitude()` used. A trailing odd byte is ignored.
     */
    fun peakAmplitude(buffer: ByteArray, length: Int = buffer.size): Int {
        var peak = 0
        var i = 0
        val end = length.coerceAtMost(buffer.size) - 1
        while (i < end) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val magnitude = abs(sample.toShort().toInt()).coerceAtMost(Short.MAX_VALUE.toInt())
            if (magnitude > peak) peak = magnitude
            i += 2
        }
        return peak
    }
}
