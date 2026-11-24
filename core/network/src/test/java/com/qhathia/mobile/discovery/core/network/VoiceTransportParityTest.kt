package com.qhathia.mobile.discovery.core.network

import com.qhathia.mobile.discovery.core.voice.transcription.toVoiceTransportFailure
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/** A voice transport failure must read the same as the same failure on any other API call. */
class VoiceTransportParityTest {

    @Test
    fun voice_and_http_classify_transport_failures_alike() {
        val failures = listOf(
            ConnectTimeoutException("connect"),
            SocketTimeoutException("socket"),
            UnknownHostException("host"),
            ConnectException("refused"),
            IOException("io"),
            IllegalStateException("other"),
        )
        failures.forEach { failure ->
            assertEquals(failure.toNetworkErrorKind().name, failure.toVoiceTransportFailure().name, failure.toString())
        }
    }
}
