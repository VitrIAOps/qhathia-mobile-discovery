package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.ConsumerField
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.model.OrderFailureDetails
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.dto.ProblemDetailDto

/**
 * Maps order failures to an [OrderException]. HTTP errors are decided by the server's `errorCode`
 * first (api-contract §38–§39) and by the status only when the body names none. Connectivity loss is
 * [OrderErrorCategory.Network] (retryable); a timeout is [OrderErrorCategory.Timeout] (the order may
 * exist, never retried).
 */
internal object OrderErrorMapper {

    fun fromHttpError(code: Int, problem: ProblemDetailDto?): OrderException {
        if (problem == null) return OrderException(fromHttpStatus(code))
        val details = OrderFailureDetails(
            productId = problem.productId,
            productName = problem.productName,
            available = problem.available,
        )
        return when (problem.resolvedErrorCode) {
            PRICE_CHANGED -> {
                val changes = problem.changes.map {
                    CartPriceChange(it.productId, it.productName, it.expectedUnitPrice, it.currentUnitPrice)
                }
                if (changes.isEmpty()) OrderException(OrderErrorCategory.OrderApi)
                else OrderException(OrderErrorCategory.PriceChanged, OrderFailureDetails(priceChanges = changes))
            }
            PRODUCT_NOT_SELLABLE -> OrderException(OrderErrorCategory.ProductNotSellable, details)
            STOCK_INSUFFICIENT -> OrderException(OrderErrorCategory.StockInsufficient, details)
            VALIDATION_ERROR -> OrderException(
                OrderErrorCategory.Validation,
                OrderFailureDetails(invalidFields = problem.fieldErrors.keys.mapNotNullTo(mutableSetOf(), ::consumerField)),
            )
            else -> OrderException(fromHttpStatus(code))
        }
    }

    /**
     * Without a usable `errorCode`: `400` and `409` mean a product cannot be sold and `404` that it is
     * gone, so the cart rechecks the basket; any other HTTP failure is a generic order failure.
     */
    private fun fromHttpStatus(code: Int): OrderErrorCategory = when (code) {
        400, 409 -> OrderErrorCategory.Unavailable
        404 -> OrderErrorCategory.NotFound
        else -> OrderErrorCategory.OrderApi
    }

    private fun consumerField(key: String): ConsumerField? = when {
        key.startsWith("customerName") -> ConsumerField.Name
        key.startsWith("customerPhone") -> ConsumerField.Phone
        key.startsWith("items") -> ConsumerField.Items
        else -> null
    }

    private const val PRICE_CHANGED = "PRICE_CHANGED"
    private const val PRODUCT_NOT_SELLABLE = "PRODUCT_NOT_SELLABLE"
    private const val STOCK_INSUFFICIENT = "STOCK_INSUFFICIENT"
    private const val VALIDATION_ERROR = "VALIDATION_ERROR"

    fun fromNetworkError(kind: NetworkErrorKind): OrderErrorCategory = when (kind) {
        NetworkErrorKind.Connectivity -> OrderErrorCategory.Network
        NetworkErrorKind.Timeout -> OrderErrorCategory.Timeout
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
