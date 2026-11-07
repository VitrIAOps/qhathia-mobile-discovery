package com.qhathia.mobile.discovery.core.domain.model

/**
 * Classification of an order-creation failure, decided from the server's `errorCode` first and its
 * HTTP status second (api-contract §38–§39). The cart reacts to each one differently.
 *
 * - [Network] — no connection: the order surely was not created, so it can be retried.
 * - [Timeout] — no answer in time: the order may exist, so it is never retried.
 * - [OrderApi] — any other server failure.
 * - [PriceChanged] — `409 PRICE_CHANGED`: a unit price changed; nothing was created.
 * - [ProductNotSellable] — `409 PRODUCT_NOT_SELLABLE` naming a product.
 * - [StockInsufficient] — `422 STOCK_INSUFFICIENT`: a line asks for more than the stock.
 * - [Validation] — `422 VALIDATION_ERROR`: a field the server rejected.
 * - [Unavailable] — `400` / `409` without a usable body: a product cannot be sold; the cart rechecks.
 * - [NotFound] — `404`: a product in the basket does not exist any more; the cart rechecks.
 */
enum class OrderErrorCategory {
    Network,
    Timeout,
    OrderApi,
    PriceChanged,
    ProductNotSellable,
    StockInsufficient,
    Validation,
    Unavailable,
    NotFound,
}

/** A consumer-data field the server can reject in `422 VALIDATION_ERROR`. */
enum class ConsumerField { Name, Phone, Items }

/**
 * What the server said about a rejected order, when it said it.
 *
 * @property priceChanges the changed lines of a `PRICE_CHANGED`.
 * @property productId the product of a `PRODUCT_NOT_SELLABLE` / `STOCK_INSUFFICIENT`.
 * @property productName its name.
 * @property available the units left of a `STOCK_INSUFFICIENT`.
 * @property invalidFields the fields of a `VALIDATION_ERROR`.
 */
data class OrderFailureDetails(
    val priceChanges: List<CartPriceChange> = emptyList(),
    val productId: String? = null,
    val productName: String? = null,
    val available: Int? = null,
    val invalidFields: Set<ConsumerField> = emptySet(),
)
