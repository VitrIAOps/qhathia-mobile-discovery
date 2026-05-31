package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the persisted [StoreSession] for observers (e.g. the Home screen). */
class ObserveStoreSessionUseCase @Inject constructor(
    private val repository: StoreSessionRepository,
) {
    operator fun invoke(): Flow<StoreSession> = repository.observe()
}
