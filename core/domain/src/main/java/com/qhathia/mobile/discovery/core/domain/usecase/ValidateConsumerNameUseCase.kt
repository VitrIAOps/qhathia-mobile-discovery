package com.qhathia.mobile.discovery.core.domain.usecase

import javax.inject.Inject

/** Why a consumer name failed validation, or `null` (from the use case) when it is valid. */
enum class ConsumerNameError {
    /** The name is blank. */
    Empty,

    /** The name has no referential name (no word with at least two letters). */
    NoReferentialName,
}

/**
 * Validates the consumer's name for the order: it must be non-blank and contain at least one
 * referential name (a token with at least two letters). Returns `null` when valid, otherwise the
 * specific [ConsumerNameError].
 */
class ValidateConsumerNameUseCase @Inject constructor() {
    operator fun invoke(name: String): ConsumerNameError? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ConsumerNameError.Empty
        val hasReferentialName = trimmed
            .split(Regex("\\s+"))
            .any { token -> token.count { it.isLetter() } >= MIN_REFERENTIAL_NAME_LETTERS }
        return if (hasReferentialName) null else ConsumerNameError.NoReferentialName
    }

    private companion object {
        const val MIN_REFERENTIAL_NAME_LETTERS = 2
    }
}
