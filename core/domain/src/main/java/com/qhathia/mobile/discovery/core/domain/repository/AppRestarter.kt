package com.qhathia.mobile.discovery.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Port that relaunches the kiosk app in a new process (remote `RESTART_APP`). */
interface AppRestarter {

    /** Whether [restart] can relaunch the app (its launch entry point resolves). */
    fun canRestart(): Boolean

    /**
     * Relaunches the app in a new process. When the shell is visible, the restart is handed to it
     * through [observeRestartRequests] (it needs an Activity to stay in the pinned task) and this
     * returns; otherwise the process is relaunched directly and this does not return.
     */
    fun restart()

    /** Restart requests for the visible shell to carry out. Collect only while the shell is started. */
    fun observeRestartRequests(): Flow<Unit>
}
