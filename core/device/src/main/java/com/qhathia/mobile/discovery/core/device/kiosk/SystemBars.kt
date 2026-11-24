package com.qhathia.mobile.discovery.core.device.kiosk

import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
