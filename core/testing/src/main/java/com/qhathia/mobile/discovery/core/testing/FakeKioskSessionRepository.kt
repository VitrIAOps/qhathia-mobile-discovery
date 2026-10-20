package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [KioskSessionRepository] for tests. */
class FakeKioskSessionRepository(initial: Boolean = false) : KioskSessionRepository {

    private val state = MutableStateFlow(initial)

    override val exitedFromSettings: StateFlow<Boolean> = state.asStateFlow()

    override fun setExitedFromSettings(exited: Boolean) {
        state.value = exited
    }
}
