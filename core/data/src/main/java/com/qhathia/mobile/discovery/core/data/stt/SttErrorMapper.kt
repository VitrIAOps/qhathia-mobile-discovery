package com.qhathia.mobile.discovery.core.data.stt

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind

/**
 * Maps network-layer failures to the domain [SttErrorCategory] taxonomy. Kept in one place so the
 * repository stays readable and the mapping is independently testable. Ktor exception types are
 * classified in `core:network`; here we only translate the resulting [NetworkErrorKind].
 */
internal object SttErrorMapper {

    /** Classifies a non-HTTP network failure (timeout vs connectivity vs unknown). */
    fun fromNetworkError(kind: NetworkErrorKind): SttErrorCategory = when (kind) {
        NetworkErrorKind.Timeout -> SttErrorCategory.Timeout
        NetworkErrorKind.Connectivity -> SttErrorCategory.Network
        NetworkErrorKind.Unknown -> SttErrorCategory.Generic
    }

    /** Classifies an HTTP error from the Qhathia token endpoint. */
    fun fromTokenHttpError(code: Int): SttErrorCategory = when (code) {
        in 500..599 -> SttErrorCategory.TranscriptionResponse // 502 Deepgram / 503 not configured
        else -> SttErrorCategory.Generic // 401/403 auth, etc.
    }

    /**
     * Classifies a failed Deepgram live stream: a refused handshake (bad/expired token, bad params) or
     * a policy/application close code (1008, 4xxx) is a provider-side rejection; any other abnormal
     * close is treated as a dropped connection; transport failures go through [fromNetworkError].
     */
    fun fromStreamFailure(closeCode: Int?, handshakeRejected: Boolean, kind: NetworkErrorKind?): SttErrorCategory =
        when {
            handshakeRejected -> SttErrorCategory.TranscriptionResponse
            closeCode != null ->
                if (closeCode == POLICY_VIOLATION || closeCode in APPLICATION_CODES) {
                    SttErrorCategory.TranscriptionResponse
                } else {
                    SttErrorCategory.Network
                }
            kind != null -> fromNetworkError(kind)
            else -> SttErrorCategory.Generic
        }

    private const val POLICY_VIOLATION = 1008
    private val APPLICATION_CODES = 4000..4999
}
