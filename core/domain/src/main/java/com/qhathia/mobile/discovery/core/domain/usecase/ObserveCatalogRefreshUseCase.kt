package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits each time a remote catalog refresh is requested while collecting. */
class ObserveCatalogRefreshUseCase @Inject constructor(
    private val signal: CatalogRefreshSignal,
) {
    operator fun invoke(): Flow<Unit> = signal.observe()
}
