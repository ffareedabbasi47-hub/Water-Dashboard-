package com.fardeen.waterdashboard.data.local

import android.content.Context
import com.fardeen.waterdashboard.model.Calibration
import com.fardeen.waterdashboard.model.HistoryPoint
import com.fardeen.waterdashboard.model.UsbConfig
import org.json.JSONArray

/** Remembers the last successfully used configuration and the sensor calibration. */
class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("aqua_core_config", Context.MODE_PRIVATE)

    fun loadConfig(): UsbConfig? {
        if (!prefs.contains("baud")) return null
        val cap = prefs.getString("capacity", null)?.toDoubleOrNull()
        return UsbConfig(prefs.getInt("baud", 9600), cap)
    }

    fun saveConfig(c: UsbConfig) {
        prefs.edit()
            .putInt("baud", c.baudRate)
            .putString("capacity", c.capacityLitres?.toString())
            .apply()
    }

    fun loadCalibration(): Calibration = Calibration(
        emptyRaw = if (prefs.contains("cal_empty")) prefs.getFloat("cal_empty", 0f) else null,
        fullRaw = if (prefs.contains("cal_full")) prefs.getFloat("cal_full", 0f) else null,
    )

    fun saveCalibration(c: Calibration) {
        val e = prefs.edit()
        c.emptyRaw?.let { e.putFloat("cal_empty", it) } ?: e.remove("cal_empty")
        c.fullRaw?.let { e.putFloat("cal_full", it) } ?: e.remove("cal_full")
        e.apply()
    }
}

/** Local history of real readings (percent values only). */
class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("aqua_core_history", Context.MODE_PRIVATE)

    fun load(): List<HistoryPoint> = try {
        val arr = JSONArray(prefs.getString("points", "[]"))
        List(arr.length()) { i ->
            val p = arr.getJSONArray(i)
            HistoryPoint(p.getLong(0), p.getDouble(1).toFloat())
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(points: List<HistoryPoint>) {
        val arr = JSONArray()
        points.forEach { arr.put(JSONArray().put(it.timestamp).put(it.percent.toDouble())) }
        prefs.edit().putString("points", arr.toString()).apply()
    }
}
