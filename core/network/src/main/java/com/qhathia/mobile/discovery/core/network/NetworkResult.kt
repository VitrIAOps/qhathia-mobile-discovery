package com.qhathia.mobile.discovery.core.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Coarse classification of a non-HTTP failure, computed here so Ktor stays inside `core:network`. */
enum class NetworkErrorKind { Timeout, Connectivity, Unknown }

/**
 * The three terminal states of any network call. Kept free of domain types — `core:data` maps this
 * to domain `Result`/error categories at the repository boundary (using [NetworkErrorKind], so it
 * never needs to import Ktor's exception types).
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class HttpError(val code: Int, val message: String) : NetworkResult<Nothing>()
    data class NetworkError(val cause: Throwable, val kind: NetworkErrorKind) : NetworkResult<Nothing>()
}

/**
 * Folds a [Result] (typically from `runCatching { ... }` around a Ktor call) into a [NetworkResult].
 * A non-2xx response surfaces as [NetworkResult.HttpError]; any other failure as
 * [NetworkResult.NetworkError] tagged with a [NetworkErrorKind].
 */
fun <T> Result<T>.toNetworkResult(): NetworkResult<T> = fold(
    onSuccess = { NetworkResult.Success(it) },
    onFailure = { throwable ->
        when (throwable) {
            is ResponseException -> NetworkResult.HttpError(
                code = throwable.response.status.value,
                message = throwable.response.status.description,
            )
            else -> NetworkResult.NetworkError(throwable, throwable.toNetworkErrorKind())
        }
    },
)

internal fun Throwable.toNetworkErrorKind(): NetworkErrorKind = when (this) {
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException,
    -> NetworkErrorKind.Timeout

    is UnknownHostException,
    is ConnectException,
    is IOException,
    -> NetworkErrorKind.Connectivity

    else -> NetworkErrorKind.Unknown
}
