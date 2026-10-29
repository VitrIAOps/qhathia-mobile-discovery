package com.qhathia.mobile.discovery.core.data.pairing

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeError
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.network.dto.DeviceInfoResponseDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceTokenResponseDto
import com.qhathia.mobile.discovery.core.network.dto.PairingStatusMessageDto
import com.qhathia.mobile.discovery.core.network.dto.PairingTokenResponseDto
import com.qhathia.mobile.discovery.core.network.service.PairingExchangeResponse
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Maps pairing DTOs to domain models, keeping serialization/time-parsing out of the domain layer. */

internal fun PairingTokenResponseDto.toDomain(): PairingCode {
    val expiry = parseInstantOrNull(expiresAt) ?: Instant.now()
    // Prefer the server's relative TTL (skew-immune). Fall back to the absolute expiry against the
    // local clock only when an older server omits it.
    val ttl = expiresInSeconds ?: Instant.now().until(expiry, ChronoUnit.SECONDS).coerceAtLeast(0)
    return PairingCode(
        code = pairingToken,
        sessionId = sessionId,
        expiresAt = expiry,
        ttlSeconds = ttl,
    )
}

internal fun DeviceTokenResponseDto.toPairedDevice(pairingCode: String?): PairedDevice = PairedDevice(
    deviceId = deviceId,
    storeId = storeId,
    deviceName = deviceName,
    platform = platform,
    status = status,
    pairingCode = pairingCode,
)

internal fun DeviceInfoResponseDto.toPairedDevice(pairingCode: String?): PairedDevice = PairedDevice(
    deviceId = deviceId,
    storeId = storeId,
    deviceName = deviceName,
    platform = platform,
    status = status,
    appVersion = appVersion,
    lastSyncAt = lastSyncAt?.let(::parseInstantOrNull),
    pairingCode = pairingCode,
)

/** Maps a raw socket status to the domain enum; returns `null` for unrecognized values. */
internal fun PairingStatusMessageDto.toDomainOrNull(): PairingStatus? = when (status.uppercase()) {
    "WAITING" -> PairingStatus.Waiting
    "APPROVED" -> PairingStatus.Approved
    "REJECTED" -> PairingStatus.Rejected
    "EXPIRED" -> PairingStatus.Expired
    else -> null
}

/**
 * Maps an exchange HTTP error to its meaning (api-contract.md §5). Without an `errorCode`, a 409 is
 * read as "not approved yet" (keep waiting) and a 410 as "expired" (new code): both are the safe guess.
 */
internal fun PairingExchangeResponse.HttpError.toExchangeError(): PairingExchangeError = when (code) {
    409 -> if (errorCode == "PAIRING_REJECTED") PairingExchangeError.Rejected else PairingExchangeError.NotApproved
    410 -> if (errorCode == "PAIRING_TOKEN_ALREADY_USED") PairingExchangeError.AlreadyUsed else PairingExchangeError.Expired
    404 -> PairingExchangeError.NotFound
    else -> PairingExchangeError.Other
}

private fun parseInstantOrNull(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()
