package com.qhathia.mobile.discovery.feature.onboarding.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.discovery.feature.onboarding.ui.InitialConfigurationScreen
import com.qhathia.mobile.discovery.feature.onboarding.ui.WelcomeScreen

/** Stable route for the first-run Welcome screen. */
const val WELCOME_ROUTE: String = "welcome"

/** Stable route for the initial device-configuration (pairing) screen. */
const val INITIAL_CONFIG_ROUTE: String = "initial_configuration"

/**
 * Registers the onboarding destinations (Welcome → initial configuration). All navigation is hoisted as
 * callbacks so the app wires the actual destinations:
 * - [onNavigateToInitialConfiguration] — the Welcome "Continuar" action.
 * - [onNavigateBackToWelcome] — the initial configuration back affordance.
 * - [onNavigateToHome] — invoked once pairing completes successfully; the app pops the onboarding
 *   destinations off the back stack and opens Home.
 */
fun NavGraphBuilder.onboardingGraph(
    onNavigateToInitialConfiguration: () -> Unit,
    onNavigateBackToWelcome: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(route = WELCOME_ROUTE) {
        WelcomeScreen(onContinue = onNavigateToInitialConfiguration, modifier = modifier)
    }
    composable(route = INITIAL_CONFIG_ROUTE) {
        InitialConfigurationScreen(
            onBack = onNavigateBackToWelcome,
            onPaired = onNavigateToHome,
            modifier = modifier,
        )
    }
}
