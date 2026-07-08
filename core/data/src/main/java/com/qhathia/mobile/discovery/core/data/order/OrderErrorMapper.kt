package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps network-layer failures to the domain [OrderErrorCategory]. Connectivity loss maps to
 * [OrderErrorCategory.Network]; a timeout or unknown failure maps to [OrderErrorCategory.OrderApi].
 * HTTP errors map to [OrderErrorCategory.OrderApi] at the repository boundary. The cart shows the same
 * simplified message for all of them.
 */
internal object OrderErrorMapper {

    fun fromNetworkError(kind: NetworkErrorKind): OrderErrorCategory = when (kind) {
        NetworkErrorKind.Connectivity -> OrderErrorCategory.Network
        NetworkErrorKind.Timeout -> OrderErrorCategory.OrderApi
        NetworkErrorKind.Unknown -> OrderErrorCategory.OrderApi
    }

    /**
     * Classifies a failure obtained while acquiring the session token (reused from the STT auth seam).
     * Only a connectivity-class STT failure maps to [OrderErrorCategory.Network]; everything else is a
     * generic order failure.
     */
    fun fromTokenFailure(cause: Throwable): OrderErrorCategory =
        if ((cause as? SttException)?.category == SttErrorCategory.Network) {
            OrderErrorCategory.Network
        } else {
            OrderErrorCategory.OrderApi
        }
}
