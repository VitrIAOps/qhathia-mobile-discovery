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

    /** Classifies an HTTP error from the Deepgram transcription call. */
    fun fromDeepgramHttpError(code: Int): SttErrorCategory = when (code) {
        REQUEST_ENTITY_TOO_LARGE -> SttErrorCategory.AudioTooLong
        else -> SttErrorCategory.TranscriptionResponse
    }

    private const val REQUEST_ENTITY_TOO_LARGE = 413
}
