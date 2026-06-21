package com.qhathia.mobile.discovery.feature.home.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.feature.home.R
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Resolves an [SttErrorCategory] to its error-screen title/message string resources. Kept in
 * `features:home` (which sees both the domain category and the home string resources) so `core:ui`
 * stays framework/domain-free.
 */
internal data class SttErrorCopy(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
)

internal fun SttErrorCategory.toCopy(): SttErrorCopy = when (this) {
    SttErrorCategory.Network -> SttErrorCopy(
        UiR.string.discovery_error_network_title,
        UiR.string.discovery_error_network_message,
    )
    SttErrorCategory.AudioTooLong -> SttErrorCopy(
        R.string.stt_error_audio_too_long_title,
        R.string.stt_error_audio_too_long_message,
    )
    SttErrorCategory.TranscriptionResponse -> SttErrorCopy(
        R.string.stt_error_transcription_title,
        R.string.stt_error_transcription_message,
    )
    SttErrorCategory.EmptyAudio -> SttErrorCopy(
        R.string.stt_error_empty_audio_title,
        R.string.stt_error_empty_audio_message,
    )
    SttErrorCategory.Timeout -> SttErrorCopy(
        R.string.stt_error_timeout_title,
        R.string.stt_error_timeout_message,
    )
    SttErrorCategory.Generic -> SttErrorCopy(
        UiR.string.discovery_error_generic_title,
        R.string.stt_error_generic_message,
    )
}
