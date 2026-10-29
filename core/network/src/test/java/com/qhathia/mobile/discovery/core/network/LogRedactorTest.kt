package com.qhathia.mobile.discovery.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogRedactorTest {

    @Test
    fun the_redacted_field_list_is_pinned() {
        // Changing this list is a privacy decision (design D10): update the spec with it.
        assertEquals(
            setOf(
                "customerName", "customerPhone", "phone", "deviceAccessToken",
                "accessToken", "token", "key", "exitPinHash",
            ),
            LogRedactor.REDACTED_FIELDS,
        )
    }

    @Test
    fun masks_each_listed_field() {
        LogRedactor.REDACTED_FIELDS.forEach { field ->
            val redacted = LogRedactor.redact("""{"$field":"secret-value","other":1}""")

            assertEquals("""{"$field":"***","other":1}""", redacted, field)
        }
    }

    @Test
    fun masks_the_share_request_body_and_keeps_the_other_fields() {
        val body = """{"customerName":"Ana Pérez","customerPhone":"987654321","items":[{"productId":"p1","quantity":2}]}"""

        val redacted = LogRedactor.redact(body)

        assertEquals("""{"customerName":"***","customerPhone":"***","items":[{"productId":"p1","quantity":2}]}""", redacted)
        assertFalse(redacted.contains("Ana"))
        assertFalse(redacted.contains("987654321"))
    }

    @Test
    fun masks_tokens_and_the_pin_hash_in_responses() {
        val settings = """{"version":4,"kioskMode":true,"exitPinHash":"${'$'}2a${'$'}10${'$'}abc/def.ghi"}"""
        val stt = """{"token": "dg-temp-123", "expiresIn": 30}"""
        val pairing = """{"deviceId":"dev-1","deviceAccessToken":"eyJhbGciOi.payload.sig","status":"ACTIVE"}"""

        assertEquals("""{"version":4,"kioskMode":true,"exitPinHash":"***"}""", LogRedactor.redact(settings))
        assertEquals("""{"token": "***", "expiresIn": 30}""", LogRedactor.redact(stt))
        assertEquals("""{"deviceId":"dev-1","deviceAccessToken":"***","status":"ACTIVE"}""", LogRedactor.redact(pairing))
    }

    @Test
    fun masks_values_with_escaped_quotes_and_numeric_values() {
        val body = """{"customerName":"Ana \"La Tienda\" Pérez","phone":987654321,"note":"ok"}"""

        assertEquals("""{"customerName":"***","phone":"***","note":"ok"}""", LogRedactor.redact(body))
    }

    @Test
    fun masks_every_occurrence_in_pretty_printed_json() {
        val body = """
            {
              "customerName" : "Ana",
              "items": [ { "customerName": "Luis" } ]
            }
        """.trimIndent()

        val redacted = LogRedactor.redact(body)

        assertFalse(redacted.contains("Ana"))
        assertFalse(redacted.contains("Luis"))
        assertTrue(redacted.contains(""""customerName" : "***""""))
    }

    @Test
    fun does_not_touch_fields_that_only_contain_a_listed_name() {
        val body = """{"tokenExpiresAt":"2026-10-29","apiKeyId":"k1","phoneVerified":true}"""

        assertEquals(body, LogRedactor.redact(body))
    }

    @Test
    fun leaves_non_json_lines_unchanged() {
        val lines = listOf(
            "REQUEST: http://87.99.156.164/api/v1/carts/share-pdf",
            "METHOD: POST",
            "BODY Content-Type: application/json",
            "RESPONSE: 201 Created",
            "token=abc phone 987654321",
        )

        lines.forEach { assertEquals(it, LogRedactor.redact(it)) }
    }
}
