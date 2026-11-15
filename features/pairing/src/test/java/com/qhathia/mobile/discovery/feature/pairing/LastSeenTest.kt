package com.qhathia.mobile.discovery.feature.pairing

import com.qhathia.mobile.discovery.feature.pairing.model.LastSeen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class LastSeenTest {

    private val now = Instant.parse("2026-11-14T12:00:00Z")

    private fun ago(minutes: Long) = LastSeen.of(now.minus(Duration.ofMinutes(minutes)), now)

    @Test
    fun given_seconds_ago_then_recently() = assertEquals(LastSeen.Recently, LastSeen.of(now.minusSeconds(20), now))

    @Test
    fun given_59_minutes_then_minutes() = assertEquals(LastSeen.Minutes(59), ago(59))

    @Test
    fun given_3_hours_then_hours() = assertEquals(LastSeen.Hours(3), ago(3 * 60 + 10))

    @Test
    fun given_25_hours_then_one_day() = assertEquals(LastSeen.Days(1), ago(25 * 60))
}
