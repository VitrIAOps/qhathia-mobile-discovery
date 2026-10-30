package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** [AppRestarter] that only counts restarts; [available] drives [canRestart]. [onRestart] runs on each restart. */
class FakeAppRestarter(
    var available: Boolean = true,
    var onRestart: () -> Unit = {},
) : AppRestarter {

    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    var restartCount: Int = 0
        private set

    override fun canRestart(): Boolean = available

    override fun restart() {
        restartCount++
        requests.tryEmit(Unit)
        onRestart()
    }

    override fun observeRestartRequests(): Flow<Unit> = requests
}
