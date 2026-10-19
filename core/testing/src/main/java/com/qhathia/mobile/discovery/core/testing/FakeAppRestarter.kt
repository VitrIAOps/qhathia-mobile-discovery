package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter

/** [AppRestarter] that only counts restarts; [available] drives [canRestart]. [onRestart] runs on each restart. */
class FakeAppRestarter(
    var available: Boolean = true,
    var onRestart: () -> Unit = {},
) : AppRestarter {

    var restartCount: Int = 0
        private set

    override fun canRestart(): Boolean = available

    override fun restart() {
        restartCount++
        onRestart()
    }
}
