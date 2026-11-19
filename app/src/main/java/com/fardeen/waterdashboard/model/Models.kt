package com.fardeen.waterdashboard.model

/** A user-facing error with a message that is safe to show as-is. */
class DeviceError(message: String) : Exception(message)

/** Serial settings + tank capacity entered on the connect screen. */
data class UsbConfig(
    val baudRate: Int,
    val capacityLitres: Double?,
)

/** Raw text fields of the connect form. */
data class ConnectionForm(
    val baud: String = "9600",
    val capacity: String = "",
) {
    fun toConfig(): UsbConfig {
        val b = baud.trim().toIntOrNull()
        if (b == null || b !in 300..2_000_000) {
            throw DeviceError("Baud rate must be a number like 9600 (must match the Arduino sketch).")
        }
        val c = capacity.trim().replace(',', '.')
        val cap = if (c.isEmpty()) null else {
            c.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
                ?: throw DeviceError("Tank capacity must be a positive number of litres.")
        }
        return UsbConfig(b, cap)
    }
}

fun UsbConfig.toForm() = ConnectionForm(
    baud = baudRate.toString(),
    capacity = capacityLitres?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
)

/**
 * Two-point calibration of the analog sensor. Both points are real sensor readings
 * captured by the user (sensor dry / sensor fully submerged).
 */
data class Calibration(val emptyRaw: Float? = null, val fullRaw: Float? = null) {
    val isValid: Boolean
        get() = emptyRaw != null && fullRaw != null && kotlin.math.abs(fullRaw - emptyRaw) >= MIN_SPAN

    /** Fill percentage 0..100 for a raw reading, or null while not calibrated. */
    fun percent(raw: Float): Float? {
        if (!isValid) return null
        val e = emptyRaw!!
        val f = fullRaw!!
        return (((raw - e) / (f - e)) * 100f).coerceIn(0f, 100f)
    }

    companion object {
        const val MIN_SPAN = 30f
    }
}

/** One real reading received from the Arduino. [percent]/[litres] are null until calibrated. */
data class WaterReading(
    val raw: Float,
    val percent: Float?,
    val litres: Float?,
    val capacityLitres: Float?,
    val timestamp: Long,
)

data class HistoryPoint(val timestamp: Long, val percent: Float)

/** State of the "Connect device" attempt (screen 1). */
sealed interface AttemptState {
    data object Idle : AttemptState
    data class Connecting(val phase: String) : AttemptState
    data class Failed(val message: String) : AttemptState
}

/** State of the live link to the Arduino (dashboard). */
sealed interface LinkState {
    data object Offline : LinkState
    data object Online : LinkState
    data object Reconnecting : LinkState
    data class Lost(val message: String) : LinkState
}
