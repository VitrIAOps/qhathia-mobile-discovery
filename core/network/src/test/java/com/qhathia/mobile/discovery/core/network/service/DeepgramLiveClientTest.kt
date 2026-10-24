package com.qhathia.mobile.discovery.core.network.service

import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Message decoding only: Ktor's MockEngine can't serve WebSockets, so the socket is verified on device. */
class DeepgramLiveClientTest {

    private val client = DeepgramLiveClient(KtorClientFactory.create(MockEngine { respondOk() }))

    @Test
    fun given_interim_results_message_when_decoded_then_transcript_and_not_final() {
        val result = client.decodeResult(
            """
            {"type":"Results","channel_index":[0,1],"duration":1.2,"start":0.0,"is_final":false,
             "speech_final":false,"channel":{"alternatives":[{"transcript":"taladro de","confidence":0.81,
             "words":[]}]},"metadata":{"request_id":"abc"}}
            """.trimIndent(),
        )

        assertEquals("taladro de", result?.transcript)
        assertFalse(result!!.isFinal)
    }

    @Test
    fun given_final_results_message_when_decoded_then_final_flags_set() {
        val result = client.decodeResult(
            """{"type":"Results","is_final":true,"speech_final":true,"channel":{"alternatives":[{"transcript":"taladro bosch","confidence":0.97}]}}""",
        )

        assertEquals("taladro bosch", result?.transcript)
        assertTrue(result!!.isFinal)
        assertTrue(result.speechFinal)
    }

    @Test
    fun given_results_without_alternatives_when_decoded_then_empty_transcript() {
        val result = client.decodeResult("""{"type":"Results","is_final":true,"channel":{"alternatives":[]}}""")

        assertEquals("", result?.transcript)
    }

    @Test
    fun given_metadata_message_when_decoded_then_ignored() {
        assertNull(
            client.decodeResult("""{"type":"Metadata","request_id":"abc","duration":3.1,"channels":1}"""),
        )
    }

    @Test
    fun given_unknown_or_malformed_message_when_decoded_then_ignored() {
        assertNull(client.decodeResult("""{"type":"SpeechStarted","timestamp":0.5}"""))
        assertNull(client.decodeResult("not json"))
    }
}
