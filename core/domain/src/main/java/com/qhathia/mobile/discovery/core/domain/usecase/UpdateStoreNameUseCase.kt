package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import javax.inject.Inject

/** Persists the active store's name into the store session. */
class UpdateStoreNameUseCase @Inject constructor(
    private val repository: StoreSessionRepository,
) {
    suspend operator fun invoke(storeName: String?) = repository.updateStoreName(storeName)
}
