package com.qhathia.mobile.discovery.core.domain.usecase

import javax.inject.Inject

/**
 * Derives a generalized keyword from an original query so an alternative search can find related
 * products when the original yields nothing (e.g. "Taladro 20v" → "Taladros").
 *
 * Deterministic and framework-free so it is trivially unit-testable. The heuristic:
 * 1. Drop qualifier tokens — pure numbers and number+unit specs (e.g. "20v", "500w", "12mm").
 * 2. Take the first remaining significant token (the head noun).
 * 3. Pluralize it with simple Spanish rules and capitalize the first letter.
 */
class GeneralizeQueryUseCase @Inject constructor() {

    operator fun invoke(query: String): String {
        val tokens = query.trim().lowercase().split(WHITESPACE).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return query.trim()

        val headNoun = tokens.firstOrNull { token -> !token.isQualifier() && token.length > MIN_NOUN_LENGTH }
            ?: tokens.first()

        return pluralize(headNoun).replaceFirstChar { it.uppercase() }
    }

    /** A spec qualifier such as "20v", "12", "500w", "8mm" — digits with an optional unit suffix. */
    private fun String.isQualifier(): Boolean = matches(QUALIFIER)

    private fun pluralize(word: String): String = when {
        word.isEmpty() || word.endsWith("s") -> word
        word.last() in VOWELS -> word + "s"
        word.endsWith("z") -> word.dropLast(1) + "ces"
        else -> word + "es"
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val QUALIFIER = Regex("\\d+[a-záéíóúñ]*")
        const val VOWELS = "aeiouáéíóú"
        const val MIN_NOUN_LENGTH = 2
    }
}
