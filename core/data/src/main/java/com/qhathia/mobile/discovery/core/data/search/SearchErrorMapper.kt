package com.qhathia.mobile.discovery.core.data.search

import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps network-layer failures to the domain [SearchErrorCategory]. Connectivity loss keeps the screen
 * with the connection message ([SearchErrorCategory.Network]); a timeout (the 30s search budget) or
 * an unknown failure is treated as a search failure ([SearchErrorCategory.SearchApi]) that triggers
 * the unhappy screen with the disguised alternative search.
 */
internal object SearchErrorMapper {

    fun fromNetworkError(kind: NetworkErrorKind): SearchErrorCategory = when (kind) {
        NetworkErrorKind.Connectivity -> SearchErrorCategory.Network
        NetworkErrorKind.Timeout -> SearchErrorCategory.SearchApi
        NetworkErrorKind.Unknown -> SearchErrorCategory.SearchApi
    }

    /**
     * Classifies a failure obtained while acquiring the session token (reused from the STT auth seam).
     * Only a connectivity-class STT failure maps to [SearchErrorCategory.Network]; everything else is
     * treated as a search failure.
     */
    fun fromTokenFailure(cause: Throwable): SearchErrorCategory =
        if ((cause as? SttException)?.category == SttErrorCategory.Network) {
            SearchErrorCategory.Network
        } else {
            SearchErrorCategory.SearchApi
        }
}
