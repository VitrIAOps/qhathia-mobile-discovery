package com.qhathia.mobile.discovery.feature.pairing.model

import java.time.Duration
import java.time.Instant

/**
 * How long ago the tablet was last seen, in the unit people use: minutes under an hour, hours under a
 * day, then days ("hace 1 día", not "hace 1440 minutos"; UX-T-22).
 */
sealed interface LastSeen {
    data object Recently : LastSeen
    data class Minutes(val value: Int) : LastSeen
    data class Hours(val value: Int) : LastSeen
    data class Days(val value: Int) : LastSeen

    companion object {
        fun of(then: Instant, now: Instant): LastSeen {
            val minutes = Duration.between(then, now).toMinutes().coerceAtLeast(0)
            return when {
                minutes < 1 -> Recently
                minutes < MINUTES_PER_HOUR -> Minutes(minutes.toInt())
                minutes < MINUTES_PER_DAY -> Hours((minutes / MINUTES_PER_HOUR).toInt())
                else -> Days((minutes / MINUTES_PER_DAY).toInt())
            }
        }

        private const val MINUTES_PER_HOUR = 60L
        private const val MINUTES_PER_DAY = 24 * 60L
    }
}
