package com.qhathia.mobile.discovery.core.data.catalog

import com.qhathia.mobile.discovery.core.domain.model.CatalogErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps network-layer failures to the domain [CatalogErrorCategory]. Connectivity loss surfaces the
 * connection-lost screen ([CatalogErrorCategory.Network]); a timeout or unknown failure is treated as
 * a generic catalog failure ([CatalogErrorCategory.Generic]).
 */
internal object CatalogErrorMapper {

    fun fromNetworkError(kind: NetworkErrorKind): CatalogErrorCategory = when (kind) {
        NetworkErrorKind.Connectivity -> CatalogErrorCategory.Network
        NetworkErrorKind.Timeout -> CatalogErrorCategory.Generic
        NetworkErrorKind.Unknown -> CatalogErrorCategory.Generic
    }

    /**
     * Classifies a failure obtained while acquiring the session token (reused from the STT auth seam).
     * Only a connectivity-class STT failure maps to [CatalogErrorCategory.Network]; everything else is
     * treated as a generic failure.
     */
    fun fromTokenFailure(cause: Throwable): CatalogErrorCategory =
        if ((cause as? SttException)?.category == SttErrorCategory.Network) {
            CatalogErrorCategory.Network
        } else {
            CatalogErrorCategory.Generic
        }
}
