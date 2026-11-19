package com.fardeen.waterdashboard.data.network

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.fardeen.waterdashboard.model.DeviceError
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume

sealed interface SerialEvent {
    data class Sample(val raw: Float) : SerialEvent
    data object Stale : SerialEvent
}

/** Talks to an Arduino (CDC-ACM, CH340, CP210x, FTDI) attached through a USB OTG cable. */
class ArduinoLink(context: Context) {
    private val appContext = context.applicationContext
    private val usb = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    @Volatile private var port: UsbSerialPort? = null
    @Volatile private var connection: UsbDeviceConnection? = null

    /**
     * Finds the first supported USB serial device, asks for permission (if [interactive]) and opens it.
     * Returns a human-readable device label.
     */
    suspend fun open(baudRate: Int, onPhase: (String) -> Unit, interactive: Boolean): String {
        close()
        onPhase("Searching for USB device…")
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usb).firstOrNull()
            ?: throw DeviceError(
                "No Arduino found on USB. Connect it with an OTG cable and make sure your phone supports USB OTG."
            )
        val device = driver.device

        if (!usb.hasPermission(device)) {
            if (!interactive) throw DeviceError("USB permission needed. Tap Reconnect and allow access.")
            onPhase("Waiting for USB permission…")
            if (!requestPermission(device)) {
                throw DeviceError("USB permission was denied. Tap Connect again and choose Allow.")
            }
        }

        onPhase("Opening serial port…")
        val conn = usb.openDevice(device)
            ?: throw DeviceError("Could not open the USB device. Unplug it, plug it back in and try again.")
        val p = driver.ports.firstOrNull()
        if (p == null) {
            conn.close()
            throw DeviceError("The USB device has no serial port.")
        }
        try {
            p.open(conn)
            p.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        } catch (e: Exception) {
            try { p.close() } catch (ignored: Exception) { }
            conn.close()
            throw DeviceError("Could not configure the serial port: ${e.message ?: "unknown error"}")
        }
        // Not every adapter supports control lines; the data link works without them.
        try { p.setDTR(true) } catch (ignored: Exception) { }
        try { p.setRTS(true) } catch (ignored: Exception) { }
        connection = conn
        port = p
        return deviceLabel(device)
    }

    fun close() {
        val p = port
        val c = connection
        port = null
        connection = null
        try { p?.close() } catch (ignored: Exception) { }
        try { c?.close() } catch (ignored: Exception) { }
    }

    /**
     * Streams real readings from the open port. Emits [SerialEvent.Stale] once when no valid line
     * arrived for [STALE_MS]. Throws [DeviceError] if the USB link breaks.
     */
    fun samples(): Flow<SerialEvent> = flow {
        val p = port ?: throw DeviceError("Serial port is not open.")
        val buf = ByteArray(1024)
        val line = StringBuilder()
        var lastValid = SystemClock.elapsedRealtime()
        var staleReported = false
        while (currentCoroutineContext().isActive) {
            val n = try {
                p.read(buf, READ_TIMEOUT_MS)
            } catch (e: IOException) {
                throw DeviceError("USB connection lost. Check the OTG cable and the Arduino.")
            }
            for (i in 0 until n) {
                val c = (buf[i].toInt() and 0xFF).toChar()
                when {
                    c == '\n' -> {
                        val v = SerialLineParser.parse(line.toString())
                        line.setLength(0)
                        if (v != null) {
                            lastValid = SystemClock.elapsedRealtime()
                            staleReported = false
                            emit(SerialEvent.Sample(v))
                        }
                    }
                    c == '\r' -> Unit
                    line.length < MAX_LINE -> line.append(c)
                    else -> line.setLength(0)
                }
            }
            if (!staleReported && SystemClock.elapsedRealtime() - lastValid > STALE_MS) {
                staleReported = true
                emit(SerialEvent.Stale)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun deviceLabel(d: UsbDevice): String {
        val name = try { d.productName } catch (ignored: Exception) { null }
        val base = name?.takeIf { it.isNotBlank() } ?: "USB serial device"
        return base + " · " + String.format("%04X:%04X", d.vendorId, d.productId)
    }

    private suspend fun requestPermission(device: UsbDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            val action = "${appContext.packageName}.USB_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != action) return
                    try { appContext.unregisterReceiver(this) } catch (ignored: Exception) { }
                    if (cont.isActive) cont.resume(usb.hasPermission(device))
                }
            }
            ContextCompat.registerReceiver(
                appContext, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(
                appContext, 0, Intent(action).setPackage(appContext.packageName), flags
            )
            cont.invokeOnCancellation {
                try { appContext.unregisterReceiver(receiver) } catch (ignored: Exception) { }
            }
            usb.requestPermission(device, pi)
        }

    companion object {
        private const val READ_TIMEOUT_MS = 500
        private const val STALE_MS = 5_000L
        private const val MAX_LINE = 256
    }
}
