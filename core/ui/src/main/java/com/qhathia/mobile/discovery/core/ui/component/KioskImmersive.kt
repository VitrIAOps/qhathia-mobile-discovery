package com.qhathia.mobile.discovery.core.ui.component

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Whether the kiosk lock is active, so app dialogs must keep the system bars hidden too (design D8).
 * Provided by the app shell; `false` in previews and tests.
 */
val LocalKioskImmersive = staticCompositionLocalOf { false }

/**
 * Hides the system bars on the window of the dialog this is called from, while [LocalKioskImmersive]
 * is on. A dialog is its own window, so the activity's immersive mode does not reach it: without this
 * the bars show over the cart, the share dialog and the keyboard. The bars are hidden again when the
 * dialog regains focus, after a layout pass in which they became visible, and (Android 10 and older)
 * whenever the system clears the hide flags, as it does when the keyboard opens (see
 * [keepSystemBarsHidden]). A swipe still shows them briefly.
 *
 * Call it once inside the content of every app dialog. Outside a dialog it does nothing.
 */
@Composable
fun KioskImmersiveDialogEffect() {
    val immersive = LocalKioskImmersive.current
    val view = LocalView.current
    DisposableEffect(view, immersive) {
        val window = view.dialogWindow()
        if (!immersive || window == null) return@DisposableEffect onDispose { }

        window.hideSystemBars()
        val stopKeepingHidden = window.keepSystemBarsHidden()
        val observer = view.viewTreeObserver
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (focused) window.hideSystemBars()
        }
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val visible = ViewCompat.getRootWindowInsets(window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.systemBars()) == true
            if (visible) window.hideSystemBars()
        }
        observer.addOnWindowFocusChangeListener(focusListener)
        observer.addOnGlobalLayoutListener(layoutListener)
        onDispose {
            stopKeepingHidden()
            val current = view.viewTreeObserver
            if (current.isAlive) {
                current.removeOnWindowFocusChangeListener(focusListener)
                current.removeOnGlobalLayoutListener(layoutListener)
            }
        }
    }
}

/** Hides the status and navigation bars on this window; a swipe shows them briefly. */
fun Window.hideSystemBars() {
    val controller = WindowCompat.getInsetsController(this, decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}

/**
 * Android 10 and older clear this window's hide flags when the keyboard opens and do not restore them
 * when it closes, which left the navigation bar over the cart (QA, API 26). This hides the bars again
 * [REHIDE_DELAY_MS] after the system clears the flags. While the keyboard is still up the system keeps
 * the navigation bar (it is needed to close the keyboard); the flags are back in place for when it
 * closes. On Android 11+ the insets controller keeps its behavior across the keyboard, so nothing is
 * added. Returns the function that stops listening.
 */
@Suppress("DEPRECATION") // The system UI visibility API is the only signal on Android 10 and older.
fun Window.keepSystemBarsHidden(): () -> Unit {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return {}
    val decor = decorView
    val rehide = Runnable { hideSystemBars() }
    decor.setOnSystemUiVisibilityChangeListener { visibility ->
        val hidden = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        if (visibility and hidden != hidden) {
            decor.removeCallbacks(rehide)
            decor.postDelayed(rehide, REHIDE_DELAY_MS)
        }
    }
    return {
        decor.removeCallbacks(rehide)
        decor.setOnSystemUiVisibilityChangeListener(null)
    }
}

private const val REHIDE_DELAY_MS = 300L

/** The window of the Compose dialog that hosts this view, or `null` outside a dialog. */
private fun View.dialogWindow(): Window? {
    var current: Any? = this
    while (current is View) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    return null
}
