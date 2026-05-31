package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [StoreSessionRepository] for tests. Emits the current session immediately and on every
 * update, mirroring the DataStore-backed production implementation.
 */
class FakeStoreSessionRepository(
    initial: StoreSession = StoreSession.EMPTY,
) : StoreSessionRepository {

    private val state = MutableStateFlow(initial)

    override fun observe(): Flow<StoreSession> = state.asStateFlow()

    override suspend fun updateStoreName(storeName: String?) {
        state.value = state.value.copy(storeName = storeName)
    }
}
