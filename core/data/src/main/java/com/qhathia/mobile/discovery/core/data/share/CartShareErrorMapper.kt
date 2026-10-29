package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps share failures to a [ShareOutcome]. A `4xx` is a final [ShareOutcome.Rejected]; a `5xx`, a
 * timeout or a connectivity loss is a retryable [ShareOutcome.Failed].
 */
internal object CartShareErrorMapper {

    fun fromHttpError(code: Int): ShareOutcome = when (code) {
        400 -> ShareOutcome.Rejected(ShareRejection.Unavailable)
        404 -> ShareOutcome.Rejected(ShareRejection.NotFound)
        422 -> ShareOutcome.Rejected(ShareRejection.Invalid)
        in 400..499 -> ShareOutcome.Rejected(ShareRejection.Other)
        else -> ShareOutcome.Failed(CartShareErrorCategory.ShareApi)
    }

    fun fromNetworkError(kind: NetworkErrorKind): ShareOutcome.Failed = ShareOutcome.Failed(
        when (kind) {
            NetworkErrorKind.Connectivity -> CartShareErrorCategory.Network
            NetworkErrorKind.Timeout, NetworkErrorKind.Unknown -> CartShareErrorCategory.ShareApi
        },
    )

    /**
     * Classifies a failure obtained while acquiring the session token (reused from the STT auth seam).
     * Only a connectivity-class STT failure maps to [CartShareErrorCategory.Network]; everything else
     * is a generic share failure. Both are retryable.
     */
    fun fromTokenFailure(cause: Throwable): ShareOutcome.Failed = ShareOutcome.Failed(
        if ((cause as? SttException)?.category == SttErrorCategory.Network) {
            CartShareErrorCategory.Network
        } else {
            CartShareErrorCategory.ShareApi
        },
    )
}
