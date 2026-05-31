package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.StoreSession

/** Canned [StoreSession] values for tests and previews. */
object StoreSessionFixtures {
    const val SAMPLE_STORE_NAME: String = "Joyería María"

    val sampleSession: StoreSession = StoreSession(storeName = SAMPLE_STORE_NAME)
}
