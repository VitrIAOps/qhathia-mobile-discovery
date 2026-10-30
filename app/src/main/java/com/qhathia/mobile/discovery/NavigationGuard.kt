package com.qhathia.mobile.discovery

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * One navigation per tap (wave 2, D1). While a transition runs, the screen on top is not `RESUMED`
 * yet, so a second tap on the same control (or "Volver" pressed again) is dropped instead of pushing a
 * duplicate screen or popping one screen too many.
 */
internal fun Lifecycle.State?.allowsNavigation(): Boolean = this == Lifecycle.State.RESUMED

/** Runs [action] on this controller only when the screen on top is settled ([allowsNavigation]). */
internal inline fun NavController.fromSettledScreen(action: NavController.() -> Unit) {
    if (currentBackStackEntry?.lifecycle?.currentState.allowsNavigation()) action()
}
