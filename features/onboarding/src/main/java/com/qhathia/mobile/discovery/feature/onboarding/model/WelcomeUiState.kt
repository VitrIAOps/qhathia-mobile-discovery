package com.qhathia.mobile.discovery.feature.onboarding.model

/**
 * What the Welcome screen shows.
 *
 * @property showUnlinkedNotice `true` after the merchant unlinked this tablet, until "Continuar".
 */
data class WelcomeUiState(
    val showUnlinkedNotice: Boolean = false,
)
