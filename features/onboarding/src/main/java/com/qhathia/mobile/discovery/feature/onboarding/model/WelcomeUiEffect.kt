package com.qhathia.mobile.discovery.feature.onboarding.model

/** One-shot effects of the Welcome screen. */
sealed interface WelcomeUiEffect {
    data object NavigateToInitialConfiguration : WelcomeUiEffect
}
