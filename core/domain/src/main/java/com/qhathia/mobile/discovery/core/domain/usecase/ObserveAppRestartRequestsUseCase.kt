package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Restart requests the app shell carries out from its Activity (design D7). */
class ObserveAppRestartRequestsUseCase @Inject constructor(
    private val appRestarter: AppRestarter,
) {
    operator fun invoke(): Flow<Unit> = appRestarter.observeRestartRequests()
}
