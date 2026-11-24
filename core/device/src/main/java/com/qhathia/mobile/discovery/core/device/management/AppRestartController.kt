package com.qhathia.mobile.discovery.core.device.management

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.system.exitProcess

/**
 * Restarts the app. While a visible screen observes [restartRequests], it carries the restart out
 * from inside its task (needed under screen pinning); otherwise the process relaunches itself.
 */
class AppRestartController(private val context: Context) {

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val restartRequests: Flow<Unit> = requests.asSharedFlow()

    fun canRestart(): Boolean = launchIntent()?.component != null

    fun restart() {
        if (requests.subscriptionCount.value > 0 && requests.tryEmit(Unit)) return
        val component = checkNotNull(launchIntent()?.component) { "No launch activity" }
        context.startActivity(Intent.makeRestartActivityTask(component))
        exitProcess(0)
    }

    private fun launchIntent(): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
}
