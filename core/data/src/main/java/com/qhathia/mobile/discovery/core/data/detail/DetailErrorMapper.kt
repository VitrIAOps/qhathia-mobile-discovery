package com.qhathia.mobile.discovery.core.data.detail

import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps network-layer failures to the domain [DetailErrorCategory]. Connectivity loss shows the
 * connection message ([DetailErrorCategory.Network]); a timeout (the 30s detail budget) or an unknown
 * failure is treated as a generic detail failure ([DetailErrorCategory.DetailApi]). HTTP 404 maps to
 * [DetailErrorCategory.NotFound] at the repository boundary.
 */
internal object DetailErrorMapper {

    fun fromNetworkError(kind: NetworkErrorKind): DetailErrorCategory = when (kind) {
        NetworkErrorKind.Connectivity -> DetailErrorCategory.Network
        NetworkErrorKind.Timeout -> DetailErrorCategory.DetailApi
        NetworkErrorKind.Unknown -> DetailErrorCategory.DetailApi
    }

    /**
     * Classifies a failure obtained while acquiring the session token (reused from the STT auth seam).
     * Only a connectivity-class STT failure maps to [DetailErrorCategory.Network]; everything else is
     * treated as a generic detail failure.
     */
    fun fromTokenFailure(cause: Throwable): DetailErrorCategory =
        if ((cause as? SttException)?.category == SttErrorCategory.Network) {
            DetailErrorCategory.Network
        } else {
            DetailErrorCategory.DetailApi
        }
}
