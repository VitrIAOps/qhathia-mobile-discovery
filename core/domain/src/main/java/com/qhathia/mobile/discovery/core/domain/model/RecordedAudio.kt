package com.qhathia.mobile.discovery.core.domain.model

/**
 * A captured audio clip ready to be transcribed.
 *
 * @property bytes the encoded audio payload (uploaded as the raw request body).
 * @property mimeType the container/codec mime (e.g. `audio/ogg`, `audio/mp4`).
 * @property durationMillis the clip length, used for debug logging and length checks.
 */
class RecordedAudio(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMillis: Long,
) {
    /** True when nothing was captured — treated as the empty/no-audio outcome. */
    val isEmpty: Boolean get() = bytes.isEmpty()
}
