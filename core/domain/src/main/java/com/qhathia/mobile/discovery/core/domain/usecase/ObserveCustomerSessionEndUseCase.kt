package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams each end of a customer session, so screens holding customer data can reset it. */
class ObserveCustomerSessionEndUseCase @Inject constructor(
    private val repository: CustomerSessionRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.sessionEnded
}
