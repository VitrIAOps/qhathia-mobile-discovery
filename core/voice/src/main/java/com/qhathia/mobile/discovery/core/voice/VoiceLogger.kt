package com.qhathia.mobile.discovery.core.voice

/** Debug output supplied by the host app, which decides whether and where it is written. */
fun interface VoiceLogger {
    fun debug(message: String)

    companion object {
        val None = VoiceLogger { }
    }
}
