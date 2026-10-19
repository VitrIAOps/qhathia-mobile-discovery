package com.qhathia.mobile.discovery.core.data.command

import android.content.Context
import android.content.Intent
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/**
 * Restarts the app as a new process: relaunches the launcher activity in a fresh task, then exits the
 * current process so singletons, scopes and the Hilt graph start over (an `Activity.recreate()` would
 * keep them alive and not be a real restart).
 */
@Singleton
class AppRestarterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppRestarter {

    override fun canRestart(): Boolean = launchIntent()?.component != null

    override fun restart() {
        val component = checkNotNull(launchIntent()?.component) { "No launch activity" }
        context.startActivity(Intent.makeRestartActivityTask(component))
        exitProcess(0)
    }

    private fun launchIntent(): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
}
