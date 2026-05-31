package com.qhathia.mobile.discovery.feature.home.model

/** One-shot navigation intents emitted by the Home screen. Destination screens are out of scope. */
sealed interface HomeUiEffect {
    data object NavigateToCatalog : HomeUiEffect
    data class NavigateToResults(val query: String) : HomeUiEffect
    data object NavigateToSettings : HomeUiEffect
}
