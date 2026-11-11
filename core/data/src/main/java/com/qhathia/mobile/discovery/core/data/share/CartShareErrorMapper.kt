package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.dto.ProblemDetailDto

/**
 * Maps share failures to a [ShareOutcome]. A `4xx` is a final [ShareOutcome.Rejected]; a `5xx`, a
 * timeout or a connectivity loss is a retryable [ShareOutcome.Failed].
 */
internal object CartShareErrorMapper {

    /**
     * Decided by the server's `errorCode` first: `PRODUCT_NOT_SELLABLE` names the unavailable product
     * and `STOCK_INSUFFICIENT` the product and the units left. Without a usable code, `400` and `409`
     * mean an unavailable product the cart has to find with a recheck.
     */
    fun fromHttpError(code: Int, problem: ProblemDetailDto? = null): ShareOutcome {
        if (problem == null) return fromHttpStatus(code)
        return when (problem.resolvedErrorCode) {
            "PRODUCT_NOT_SELLABLE" ->
                ShareOutcome.Rejected(
                    ShareRejection.Unavailable,
                    problem.productId,
                    problem.productName,
                    reason = problem.reason,
                )
            "STOCK_INSUFFICIENT" ->
                ShareOutcome.Rejected(ShareRejection.StockInsufficient, problem.productId, problem.productName, problem.available)
            else -> fromHttpStatus(code)
        }
    }

    private fun fromHttpStatus(code: Int): ShareOutcome = when (code) {
        400, 409 -> ShareOutcome.Rejected(ShareRejection.Unavailable)
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
