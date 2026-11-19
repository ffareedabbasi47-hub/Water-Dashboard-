package com.fardeen.waterdashboard.data.repository

import com.fardeen.waterdashboard.data.local.ConfigStore
import com.fardeen.waterdashboard.data.local.HistoryStore
import com.fardeen.waterdashboard.data.network.ArduinoLink
import com.fardeen.waterdashboard.data.network.SerialEvent
import com.fardeen.waterdashboard.model.Calibration
import com.fardeen.waterdashboard.model.HistoryPoint
import com.fardeen.waterdashboard.model.UsbConfig
import com.fardeen.waterdashboard.model.WaterReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Single source of truth: real sensor readings, calibration, config and local history. */
class WaterRepository(
    private val link: ArduinoLink,
    private val configStore: ConfigStore,
    private val historyStore: HistoryStore,
) {
    private val _history = MutableStateFlow(historyStore.load())
    val history: StateFlow<List<HistoryPoint>> = _history.asStateFlow()

    fun lastConfig(): UsbConfig? = configStore.loadConfig()
    fun saveConfig(config: UsbConfig) = configStore.saveConfig(config)
    fun calibration(): Calibration = configStore.loadCalibration()

    suspend fun saveCalibration(c: Calibration) {
        configStore.saveCalibration(c)
        clearHistory() // old percentages were computed with the previous calibration
    }

    suspend fun open(baud: Int, onPhase: (String) -> Unit, interactive: Boolean): String =
        link.open(baud, onPhase, interactive)

    fun samples(): Flow<SerialEvent> = link.samples()
    fun close() = link.close()

    /** Turns a raw sensor value into a reading and, if calibrated, records it in the history. */
    suspend fun record(raw: Float, config: UsbConfig, calibration: Calibration, now: Long): WaterReading {
        val percent = calibration.percent(raw)
        val cap = config.capacityLitres?.toFloat()
        val litres = if (percent != null && cap != null) percent / 100f * cap else null
        if (percent != null) {
            val next = (_history.value + HistoryPoint(now, percent)).takeLast(MAX_HISTORY)
            _history.value = next
            withContext(Dispatchers.IO) { historyStore.save(next) }
        }
        return WaterReading(raw, percent, litres, cap, now)
    }

    suspend fun clearHistory() {
        _history.update { emptyList() }
        withContext(Dispatchers.IO) { historyStore.save(emptyList()) }
    }

    private companion object {
        const val MAX_HISTORY = 300
    }
}
