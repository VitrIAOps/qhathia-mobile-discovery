package com.qhathia.mobile.discovery

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.BrightnessMode
import com.qhathia.mobile.discovery.core.domain.model.KioskLockState
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.component.LocalVoiceAssistantEnabled
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.feature.cart.ui.CartDialog
import com.qhathia.mobile.discovery.feature.catalog.navigation.catalogRoute
import com.qhathia.mobile.discovery.feature.catalog.navigation.catalogScreen
import com.qhathia.mobile.discovery.feature.home.navigation.HOME_ROUTE
import com.qhathia.mobile.discovery.feature.home.navigation.homeScreen
import com.qhathia.mobile.discovery.feature.onboarding.navigation.INITIAL_CONFIG_ROUTE
import com.qhathia.mobile.discovery.feature.onboarding.navigation.WELCOME_ROUTE
import com.qhathia.mobile.discovery.feature.onboarding.navigation.onboardingGraph
import com.qhathia.mobile.discovery.feature.productdetail.navigation.productDetailRoute
import com.qhathia.mobile.discovery.feature.productdetail.navigation.productDetailScreen
import com.qhathia.mobile.discovery.feature.productsearch.navigation.productSearchRoute
import com.qhathia.mobile.discovery.feature.productsearch.navigation.productSearchScreen
import com.qhathia.mobile.discovery.feature.settings.navigation.NETWORK_SETTINGS_ROUTE
import com.qhathia.mobile.discovery.feature.settings.navigation.networkSettingsScreen
import com.qhathia.mobile.discovery.feature.settings.ui.SettingsPasswordGate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled at point of use */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Business rule: request microphone access when the app opens so voice search is ready.
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            DiscoveryAppRoot()
        }
    }
}

@Composable
private fun DiscoveryAppRoot(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val deviceSettings by mainViewModel.deviceSettings.collectAsStateWithLifecycle()
    val kioskLock by mainViewModel.kioskLock.collectAsStateWithLifecycle()
    ApplyWindowBrightness(deviceSettings.brightnessMode)
    ApplyKioskLock(kioskLock)

    // Remote "Asistente por voz": when off, every MicButton renders nothing.
    CompositionLocalProvider(LocalVoiceAssistantEnabled provides deviceSettings.voiceAssistantEnabled) {
        DiscoveryAppContent(mainViewModel)
    }
}

@Composable
private fun DiscoveryAppContent(mainViewModel: MainViewModel) {
    DiscoveryTheme {
        val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

        // Resolve the start route from the persisted pairing state before building the NavHost, so an
        // unpaired device never flashes Home. While the session is still loading, show a neutral splash.
        when (startDestination) {
            StartDestination.Loading -> StartupSplash()
            StartDestination.Onboarding -> DiscoveryNavHost(startRoute = WELCOME_ROUTE, mainViewModel = mainViewModel)
            StartDestination.Home -> DiscoveryNavHost(startRoute = HOME_ROUTE, mainViewModel = mainViewModel)
        }
    }
}

/**
 * Applies the remote "Brillo" setting to this window: AUTO leaves the system in control; LOW/MEDIUM/HIGH
 * override the window brightness (no WRITE_SETTINGS needed, and it reverts when the app leaves).
 */
@Composable
private fun ApplyWindowBrightness(mode: BrightnessMode) {
    val activity = LocalActivity.current ?: return
    LaunchedEffect(mode) {
        val window = activity.window
        window.attributes = window.attributes.apply { screenBrightness = mode.windowBrightness() }
    }
}

private fun BrightnessMode.windowBrightness(): Float = when (this) {
    BrightnessMode.AUTO -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    BrightnessMode.LOW -> 0.3f
    BrightnessMode.MEDIUM -> 0.6f
    BrightnessMode.HIGH -> 1f
}

/**
 * Applies the kiosk lock to this activity on every resume and whenever it changes (design D5): screen
 * pinning without Device Owner, hidden system bars (a swipe shows them transiently) and screen kept on.
 * Re-applying on resume also pins again after the merchant or a customer unpinned with the system
 * gesture.
 */
@Composable
private fun ApplyKioskLock(lock: KioskLockState) {
    val activity = LocalActivity.current ?: return
    LifecycleResumeEffect(lock.active) {
        activity.applyKioskLock(lock.active)
        onPauseOrDispose { }
    }
}

private fun Activity.applyKioskLock(active: Boolean) {
    val pinned = getSystemService(ActivityManager::class.java).lockTaskModeState !=
        ActivityManager.LOCK_TASK_MODE_NONE
    val insets = WindowCompat.getInsetsController(window, window.decorView)
    if (active) {
        // Only ask when not pinned yet, so an already pinned task is not asked again.
        if (!pinned) runCatching { startLockTask() }
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        if (pinned) runCatching { stopLockTask() }
        insets.show(WindowInsetsCompat.Type.systemBars())
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

/** Neutral, themed placeholder shown while the persisted session is being read. */
@Composable
private fun StartupSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryNavHost(
    startRoute: String,
    mainViewModel: MainViewModel,
) {
    val navController = rememberNavController()

    // Freeze the resolved start route once so a later pairing (session flips to paired) does not try to
    // rebuild the graph; post-pairing navigation is handled by the onboarding onPaired callback.
    val startDestinationRoute = rememberSaveable { startRoute }

    // Cart is a dialog overlay hosted here (feature modules cannot depend on each other), opened
    // by the cart controls on the search and detail screens.
    var showCart by rememberSaveable { mutableStateOf(false) }

    // Settings PIN gate is an overlay hosted here so the PIN is requested before entering settings;
    // a correct PIN navigates straight to the Network settings screen.
    var showSettingsGate by rememberSaveable { mutableStateOf(false) }

    // Off-Home inactivity: any touch-down anywhere bumps the tick, restarting the 1-minute timer.
    var interactionTick by remember { mutableIntStateOf(0) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val kioskLock by mainViewModel.kioskLock.collectAsStateWithLifecycle()

    // Kiosk: a temporary exit from Configuración ends when the merchant is back on Home (any way back:
    // the side-menu "Volver" or system Back), so the lock applies again.
    LaunchedEffect(currentRoute) {
        if (currentRoute == HOME_ROUTE) mainViewModel.resumeKioskMode()
    }

    // Kiosk: Back on Home would finish the root activity, which ends screen pinning; swallow it.
    BackHandler(enabled = kioskLock.active && currentRoute == HOME_ROUTE) { }

    // Business rule: clear the cart after 1 minute of inactivity on any screen that isn't Home.
    LaunchedEffect(currentRoute, interactionTick) {
        if (currentRoute != null && currentRoute != HOME_ROUTE) {
            delay(CartConstants.INACTIVITY_TIMEOUT_SECONDS * 1_000L)
            mainViewModel.clearCart()
        }
    }

    // Kiosk: keep content out of the system-bar areas even while the bars are hidden. Hidden bars report
    // zero insets, which moved the bottom controls (the settings gear) into the navigation-bar strip,
    // where a pinned task does not receive touches.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    // Observe-only (Initial pass, no consume) so the timer resets without
                    // intercepting taps/scrolls meant for the screen below.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                                interactionTick++
                            }
                        }
                    }
                },
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestinationRoute,
            ) {
                onboardingGraph(
                    onNavigateToInitialConfiguration = { navController.navigate(INITIAL_CONFIG_ROUTE) },
                    onNavigateBackToWelcome = { navController.popBackStack() },
                    onNavigateToHome = {
                        // Business rule: only a successful pairing advances to Home; drop the onboarding
                        // destinations so Back from Home does not re-enter the welcome flow.
                        navController.navigate(HOME_ROUTE) {
                            popUpTo(WELCOME_ROUTE) { inclusive = true }
                        }
                    },
                )
                homeScreen(
                    onNavigateToCatalog = { navController.navigate(catalogRoute()) },
                    onNavigateToProductSearch = { query, source ->
                        navController.navigate(productSearchRoute(query, source))
                    },
                    onNavigateToSettings = { showSettingsGate = true },
                )
                catalogScreen(
                    onNavigateHomeIdle = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                    onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) },
                    onNavigateToSearch = { query, source ->
                        navController.navigate(productSearchRoute(query, source))
                    },
                    onOpenCart = { showCart = true },
                )
                productSearchScreen(
                    onNavigateHomeIdle = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                    onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) },
                    onOpenCart = { showCart = true },
                )
                productDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHomeIdle = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                    onNavigateToSearch = { query ->
                        navController.navigate(productSearchRoute(query, SearchSource.Text))
                    },
                    onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) },
                    onOpenCart = { showCart = true },
                )
                networkSettingsScreen(
                    onNavigateBack = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                )
            }
        }
    }

    if (showCart) {
        CartDialog(
            onNavigateToDetail = { productId ->
                showCart = false
                navController.navigate(productDetailRoute(productId))
            },
            onDismiss = { showCart = false },
            onReturnHome = {
                // Order placed (accepted) or cancelled from the error state: the session was
                // cleared in the ViewModel; close the overlay and pop back to Home.
                showCart = false
                navController.popBackStack(HOME_ROUTE, inclusive = false)
            },
        )
    }

    if (showSettingsGate) {
        SettingsPasswordGate(
            onUnlocked = {
                showSettingsGate = false
                navController.navigate(NETWORK_SETTINGS_ROUTE)
            },
            onDismiss = { showSettingsGate = false },
        )
    }
}
