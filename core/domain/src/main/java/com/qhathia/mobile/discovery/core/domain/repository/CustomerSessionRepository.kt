package com.qhathia.mobile.discovery.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Port for one customer's session (design D2). The basket lives in [CartRepository]; this port names
 * the current customer ([currentSessionId]) and signals the end of their session, so every screen that
 * holds customer data (the cart form, the product share form) can reset itself. Not persisted: a
 * process restart already starts a clean session.
 */
interface CustomerSessionRepository {

    /** Emits once each time a customer session ends. Late subscribers do not get past ends. */
    val sessionEnded: Flow<Unit>

    /**
     * Random id of the current customer's session, created on first use and kept until [end]. Sent with
     * that customer's searches, product views and order so the server counts each customer once
     * (add-customer-session-id, F-22). Carries no customer data.
     */
    fun currentSessionId(): String

    /** Signals that the current customer session ended; the next customer gets a new id. */
    suspend fun end()
}
