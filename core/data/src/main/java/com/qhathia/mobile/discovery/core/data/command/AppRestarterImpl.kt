package com.qhathia.mobile.discovery.core.data.command

import android.content.Context
import android.content.Intent
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/**
 * Restarts the app as a new process (design D7). When the shell is visible (it collects
 * [observeRestartRequests]), the restart is handed to it: it launches a trampoline activity inside
 * the pinned task, which kills this process and starts the shell again, so pinning holds. Otherwise
 * (no visible shell, nothing pinned) it relaunches the launcher activity in a fresh task and exits.
 * Either way singletons, scopes and the Hilt graph start over, unlike `Activity.recreate()`.
 */
@Singleton
class AppRestarterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppRestarter {

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun canRestart(): Boolean = launchIntent()?.component != null

    override fun restart() {
        if (requests.subscriptionCount.value > 0 && requests.tryEmit(Unit)) return
        val component = checkNotNull(launchIntent()?.component) { "No launch activity" }
        context.startActivity(Intent.makeRestartActivityTask(component))
        exitProcess(0)
    }

    override fun observeRestartRequests(): Flow<Unit> = requests.asSharedFlow()

    private fun launchIntent(): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
}
