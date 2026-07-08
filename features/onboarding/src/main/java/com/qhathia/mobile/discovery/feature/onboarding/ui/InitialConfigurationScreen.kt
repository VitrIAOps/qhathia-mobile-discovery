package com.qhathia.mobile.discovery.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.feature.onboarding.R
import com.qhathia.mobile.discovery.feature.pairing.ui.PairingScreen

/**
 * Initial device-configuration screen. Composes the shared `:features:pairing` slice with the "QHATHIA"
 * header, a back affordance that returns to the Welcome screen ([onBack]), and no settings side menu.
 * A successful pairing drives [onPaired] (the host navigates to Home). All pairing validations are the
 * pairing slice's own, so they behave identically to the Settings "Vincular" section.
 */
@Composable
fun InitialConfigurationScreen(
    onBack: () -> Unit,
    onPaired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PairingScreen(
        title = stringResource(R.string.onboarding_pairing_title),
        modifier = modifier,
        showBackButton = true,
        onBack = onBack,
        onPaired = onPaired,
    )
}
