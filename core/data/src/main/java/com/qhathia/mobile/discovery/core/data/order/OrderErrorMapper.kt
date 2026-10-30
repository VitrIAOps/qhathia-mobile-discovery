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

    /**
     * `400` and `409` (`PRODUCT_NOT_SELLABLE`) mean a product is out of stock, inactive or not
     * `AVAILABLE` (the server's only other `400`, a duplicated `productId`, cannot happen: the basket
     * merges lines). `404` means a product is gone. Any other HTTP failure is a generic order failure.
     * The error body is not read: the cart recheck finds the affected lines itself, and falls back to
     * the generic error when it finds none.
     */
    fun fromHttpError(code: Int): OrderErrorCategory = when (code) {
        400, 409 -> OrderErrorCategory.Unavailable
        404 -> OrderErrorCategory.NotFound
        else -> OrderErrorCategory.OrderApi
    }

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
