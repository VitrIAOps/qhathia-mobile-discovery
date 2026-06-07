package com.qhathia.mobile.discovery.core.domain.model

/**
 * Where a product search originated. Drives presentation differences — notably whether the
 * microphone animation is retained while the search is in progress (kept for [Voice], hidden for
 * [Text]).
 */
enum class SearchSource { Voice, Text }
