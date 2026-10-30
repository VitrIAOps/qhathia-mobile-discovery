package com.qhathia.mobile.discovery.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Port for the end of one customer's session (design D2). The basket lives in [CartRepository]; this
 * port only signals the end, so every screen that holds customer data (the cart form, the product
 * share form) can reset itself. Not persisted: a process restart already starts a clean session.
 */
interface CustomerSessionRepository {

    /** Emits once each time a customer session ends. Late subscribers do not get past ends. */
    val sessionEnded: Flow<Unit>

    /** Signals that the current customer session ended. */
    suspend fun end()
}
