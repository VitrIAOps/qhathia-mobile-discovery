package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import javax.inject.Inject

/** Dismisses the "this tablet was unlinked" notice shown on Welcome. */
class DismissUnlinkedNoticeUseCase @Inject constructor(
    private val storeSessionRepository: StoreSessionRepository,
) {
    suspend operator fun invoke() = storeSessionRepository.dismissUnlinkedNotice()
}
