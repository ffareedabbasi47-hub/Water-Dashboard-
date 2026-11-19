package com.fardeen.waterdashboard.data.network

import org.json.JSONObject

/**
 * Parses one line sent by the Arduino into a raw sensor value.
 * Accepted: `{"raw":312.4}`, `312`, `312.4`, `raw=312`, `A0: 312`, `AQUA,312`.
 * Anything else (boot noise, wrong baud rate garbage) returns null and is ignored.
 */
object SerialLineParser {
    private val keyed = Regex("""^[A-Za-z_][A-Za-z_0-9]*\s*[,=:]\s*(-?\d+(?:\.\d+)?)$""")
    private val jsonKeys = listOf("raw", "value", "adc", "level")

    fun parse(line: String): Float? {
        val s = line.trim()
        if (s.isEmpty()) return null
        val v: Float? = when {
            s.startsWith("{") -> parseJson(s)
            else -> s.toFloatOrNull() ?: keyed.matchEntire(s)?.groupValues?.get(1)?.toFloatOrNull()
        }
        return v?.takeIf { it.isFinite() && it in 0f..65535f }
    }

    private fun parseJson(s: String): Float? = try {
        val o = JSONObject(s)
        jsonKeys.firstNotNullOfOrNull { k ->
            if (o.has(k)) o.optDouble(k, Double.NaN).takeIf { !it.isNaN() }?.toFloat() else null
        }
    } catch (e: org.json.JSONException) {
        null
    }
}
