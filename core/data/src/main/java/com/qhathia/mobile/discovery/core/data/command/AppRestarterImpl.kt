package com.qhathia.mobile.discovery.core.data.command

import android.content.Context
import com.qhathia.mobile.discovery.core.device.management.AppRestartController
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** [AppRestarter] over `core:device`'s restart controller. */
@Singleton
class AppRestarterImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AppRestarter {

    private val controller = AppRestartController(context)

    override fun canRestart(): Boolean = controller.canRestart()

    override fun restart() = controller.restart()

    override fun observeRestartRequests(): Flow<Unit> = controller.restartRequests
}
