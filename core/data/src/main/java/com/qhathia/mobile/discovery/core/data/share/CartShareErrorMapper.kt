package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps network-layer failures to the domain [CartShareErrorCategory]. Connectivity loss maps to
 * [CartShareErrorCategory.Network]; a timeout or unknown failure maps to
 * [CartShareErrorCategory.ShareApi]. HTTP errors map to [CartShareErrorCategory.ShareApi] at the
 * repository boundary. The cart shows the same simplified message for all of them.
 */
internal object CartShareErrorMapper {

    fun fromNetworkError(kind: NetworkErrorKind): CartShareErrorCategory = when (kind) {
        NetworkErrorKind.Connectivity -> CartShareErrorCategory.Network
        NetworkErrorKind.Timeout -> CartShareErrorCategory.ShareApi
        NetworkErrorKind.Unknown -> CartShareErrorCategory.ShareApi
    }

    /**
     * Classifies a failure obtained while acquiring the session token (reused from the STT auth seam).
     * Only a connectivity-class STT failure maps to [CartShareErrorCategory.Network]; everything else
     * is a generic share failure.
     */
    fun fromTokenFailure(cause: Throwable): CartShareErrorCategory =
        if ((cause as? SttException)?.category == SttErrorCategory.Network) {
            CartShareErrorCategory.Network
        } else {
            CartShareErrorCategory.ShareApi
        }
}
