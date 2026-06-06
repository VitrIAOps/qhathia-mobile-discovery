package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed voice-search [Result], tagging it with the [SttErrorCategory] so the
 * presentation layer can show the matching error screen without inspecting low-level causes.
 */
class SttException(val category: SttErrorCategory) : Exception("Speech-to-text failed: $category")
