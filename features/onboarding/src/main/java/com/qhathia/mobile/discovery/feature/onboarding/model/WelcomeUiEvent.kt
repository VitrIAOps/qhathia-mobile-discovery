package com.qhathia.mobile.discovery.feature.onboarding.model

/** User intents on the Welcome screen. */
sealed interface WelcomeUiEvent {
    /** "Continuar": dismiss the unlink notice, then go to the initial configuration. */
    data object Continue : WelcomeUiEvent
}
