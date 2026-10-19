package com.qhathia.mobile.discovery.core.domain.repository

/** Port that relaunches the kiosk app in a new process (remote `RESTART_APP`). */
interface AppRestarter {

    /** Whether [restart] can relaunch the app (its launch entry point resolves). */
    fun canRestart(): Boolean

    /** Relaunches the app and ends the current process. Does not return when it succeeds. */
    fun restart()
}
