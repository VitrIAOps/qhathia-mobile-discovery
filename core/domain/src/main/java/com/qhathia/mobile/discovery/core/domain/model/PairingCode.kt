package com.qhathia.mobile.discovery.core.domain.model

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A generated pairing code together with its opaque session and server-authoritative expiry.
 *
 * @property code the human-readable code shown on the tablet (e.g. "2Y567").
 * @property sessionId opaque session id used to subscribe to status and exchange the device token.
 * @property expiresAt the instant the code expires (TTL ≤ 5 min, see [PairingConstants]).
 * @property ttlSeconds the server-granted lifetime in seconds (`expiresAt − createdAt`). Seed the
 *   countdown from this rather than [remainingSeconds] so it is immune to device↔server clock skew.
 */
data class PairingCode(
    val code: String,
    val sessionId: String,
    val expiresAt: Instant,
    val ttlSeconds: Long,
) {
    /** Remaining lifetime in whole seconds, never negative, computed against [now]. */
    fun remainingSeconds(now: Instant = Instant.now()): Long =
        now.until(expiresAt, ChronoUnit.SECONDS).coerceAtLeast(0)

    /** Whether the code has reached or passed its expiry at [now]. */
    fun isExpired(now: Instant = Instant.now()): Boolean = !now.isBefore(expiresAt)
}
