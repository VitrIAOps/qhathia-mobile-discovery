package com.qhathia.mobile.discovery.core.network

/**
 * Masks personal data and secrets in network log lines (design D10, RED-060). Every build that logs
 * network calls (debug and `profile`) passes each line through [redact] before it reaches logcat.
 *
 * The value of each field in [REDACTED_FIELDS] is replaced by [MASK] wherever it appears as a JSON
 * member (`"field": "value"` or `"field": 123`), in requests and responses. Other fields are kept, so
 * QA still sees the shape of each call. Text that is not JSON is left unchanged.
 *
 * `exitPinHash` is listed because a 4-digit PIN can be recovered from its hash by trying all 10,000
 * values.
 */
internal object LogRedactor {

    const val MASK = "***"

    /** JSON members whose values are never logged. */
    val REDACTED_FIELDS: Set<String> = setOf(
        "customerName",
        "customerPhone",
        "phone",
        "deviceAccessToken",
        "accessToken",
        "token",
        "key",
        "exitPinHash",
    )

    private val member = Regex(
        // "field" : "string value with \" escapes"   |   "field" : number / true / false / null
        "\"(" + REDACTED_FIELDS.joinToString("|") { Regex.escape(it) } + ")\"(\\s*):(\\s*)" +
            "(\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|true|false|null)",
    )

    /** Returns [message] with the value of every [REDACTED_FIELDS] member replaced by [MASK]. */
    fun redact(message: String): String =
        member.replace(message) { match ->
            val (field, beforeColon, afterColon) = match.destructured
            "\"$field\"$beforeColon:$afterColon\"$MASK\""
        }
}
