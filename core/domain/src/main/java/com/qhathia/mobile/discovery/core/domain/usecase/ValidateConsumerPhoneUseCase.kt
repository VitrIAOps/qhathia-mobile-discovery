package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.CartConstants
import javax.inject.Inject

/** Why a consumer phone failed validation, or `null` (from the use case) when it is valid. */
enum class ConsumerPhoneError {
    /** The phone is blank. */
    Empty,

    /** The phone does not match the general mobile-number format. */
    InvalidFormat,
}

/**
 * Validates the consumer's cell-phone number against the general mobile format
 * ([CartConstants.PHONE_REGEX]). Whitespace is ignored so spaced input (e.g. `987 654 321`) is
 * accepted. Returns `null` when valid, otherwise the specific [ConsumerPhoneError].
 */
class ValidateConsumerPhoneUseCase @Inject constructor() {
    operator fun invoke(phone: String): ConsumerPhoneError? {
        val normalized = phone.filterNot { it.isWhitespace() }
        if (normalized.isEmpty()) return ConsumerPhoneError.Empty
        return if (CartConstants.PHONE_REGEX.matches(normalized)) null else ConsumerPhoneError.InvalidFormat
    }
}
