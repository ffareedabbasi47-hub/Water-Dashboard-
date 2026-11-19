package com.fardeen.waterdashboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fardeen.waterdashboard.data.local.ConfigStore
import com.fardeen.waterdashboard.data.local.HistoryStore
import com.fardeen.waterdashboard.data.network.ArduinoLink
import com.fardeen.waterdashboard.data.network.SerialEvent
import com.fardeen.waterdashboard.data.repository.WaterRepository
import com.fardeen.waterdashboard.model.AttemptState
import com.fardeen.waterdashboard.model.ConnectionForm
import com.fardeen.waterdashboard.model.DeviceError
import com.fardeen.waterdashboard.model.LinkState
import com.fardeen.waterdashboard.model.UsbConfig
import com.fardeen.waterdashboard.model.toForm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AquaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WaterRepository(ArduinoLink(app), ConfigStore(app), HistoryStore(app))

    val history = repo.history

    private val _ui = MutableStateFlow(
        UiState(
            form = repo.lastConfig()?.toForm() ?: ConnectionForm(),
            calibration = repo.calibration(),
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var connectJob: Job? = null
    private var sessionJob: Job? = null
    private val recentRaw = ArrayDeque<Float>()

    // ---------------------------------------------------------------- form

    fun updateForm(form: ConnectionForm) = _ui.update { it.copy(form = form) }

    // ------------------------------------------------------------- connect

    /** Opens the USB port and only shows the dashboard once real sensor data has arrived. */
    fun connect() {
        if (_ui.value.attempt is AttemptState.Connecting) return
        val config = try {
            _ui.value.form.toConfig()
        } catch (e: DeviceError) {
            _ui.update { it.copy(attempt = AttemptState.Failed(e.message ?: "Invalid input.")) }
            return
        }
        sessionJob?.cancel()
        sessionJob = null
        _ui.update { it.copy(attempt = AttemptState.Connecting("Preparing…")) }

        connectJob = viewModelScope.launch {
            try {
                val label = repo.open(config.baudRate, ::setPhase, interactive = true)
                setPhase("Waiting for sensor data… (the Arduino restarts when connected)")
                val first = withTimeoutOrNull(VERIFY_TIMEOUT_MS) {
                    repo.samples().filterIsInstance<SerialEvent.Sample>().first()
                } ?: throw DeviceError(
                    "Arduino found, but it sent no valid sensor data. Upload the Aqua Core sketch " +
                        "and check the baud rate (${config.baudRate})."
                )
                repo.saveConfig(config)
                pushRaw(first.raw)
                val reading = repo.record(first.raw, config, _ui.value.calibration, System.currentTimeMillis())
                _ui.update {
                    it.copy(
                        screen = Screen.Dashboard,
                        showSettings = false,
                        attempt = AttemptState.Idle,
                        link = LinkState.Online,
                        reading = reading,
                        activeConfig = config,
                        deviceLabel = label,
                        form = config.toForm(),
                    )
                }
                startSession(config, reopenFirst = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                repo.close()
                val msg = e.message ?: "Connection failed."
                _ui.update { it.copy(attempt = AttemptState.Failed(msg)) }
                // Settings were opened over a running dashboard: bring the old session back.
                val old = _ui.value.activeConfig
                if (old != null && _ui.value.screen == Screen.Dashboard) {
                    _ui.update { it.copy(link = LinkState.Lost(msg)) }
                    startSession(old, reopenFirst = true)
                }
            }
        }
    }

    private fun setPhase(phase: String) = _ui.update {
        if (it.attempt is AttemptState.Connecting) it.copy(attempt = AttemptState.Connecting(phase)) else it
    }

    // ------------------------------------------------------------- session

    /** Reads sensor data; on connection loss keeps retrying every few seconds. */
    private fun startSession(config: UsbConfig, reopenFirst: Boolean, interactiveFirst: Boolean = false) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            var needOpen = reopenFirst
            var interactive = interactiveFirst
            while (isActive) {
                try {
                    if (needOpen) {
                        if (interactive) _ui.update { it.copy(link = LinkState.Reconnecting) }
                        val label = repo.open(config.baudRate, {}, interactive)
                        _ui.update { it.copy(deviceLabel = label) }
                        needOpen = false
                    }
                    repo.samples().collect { handle(it, config) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    repo.close()
                    needOpen = true
                    interactive = false
                    _ui.update { it.copy(link = LinkState.Lost(e.message ?: "Connection lost.")) }
                    delay(RETRY_MS)
                }
            }
        }
    }

    private suspend fun handle(event: SerialEvent, config: UsbConfig) {
        when (event) {
            is SerialEvent.Sample -> {
                pushRaw(event.raw)
                val r = repo.record(event.raw, config, _ui.value.calibration, System.currentTimeMillis())
                _ui.update { it.copy(reading = r, link = LinkState.Online) }
            }
            SerialEvent.Stale -> _ui.update {
                it.copy(link = LinkState.Lost("No valid data from the Arduino for 5 s. Check the sketch and baud rate."))
            }
        }
    }

    private fun pushRaw(raw: Float) {
        recentRaw.addLast(raw)
        while (recentRaw.size > 5) recentRaw.removeFirst()
    }

    // ---------------------------------------------------------- menu actions

    fun reconnect() {
        val c = _ui.value.activeConfig ?: return
        connectJob?.cancel()
        startSession(c, reopenFirst = true, interactiveFirst = true)
    }

    fun disconnect() {
        connectJob?.cancel()
        sessionJob?.cancel()
        connectJob = null
        sessionJob = null
        repo.close()
        recentRaw.clear()
        _ui.update {
            it.copy(
                screen = Screen.Connect,
                attempt = AttemptState.Idle,
                link = LinkState.Offline,
                reading = null,
                activeConfig = null,
                deviceLabel = null,
                showSettings = false,
                showCalibration = false,
            )
        }
    }

    fun openSettings() = _ui.update { it.copy(showSettings = true, attempt = AttemptState.Idle) }
    fun closeSettings() = _ui.update { it.copy(showSettings = false, attempt = AttemptState.Idle) }

    // ----------------------------------------------------------- calibration

    fun openCalibration() = _ui.update { it.copy(showCalibration = true) }
    fun closeCalibration() = _ui.update { it.copy(showCalibration = false) }

    /** Stores the current (averaged) real sensor reading as the EMPTY point. */
    fun setEmptyPoint() = setPoint(empty = true)

    /** Stores the current (averaged) real sensor reading as the FULL point. */
    fun setFullPoint() = setPoint(empty = false)

    private fun setPoint(empty: Boolean) {
        if (recentRaw.isEmpty()) return
        val avg = recentRaw.average().toFloat()
        val cur = _ui.value.calibration
        val next = if (empty) cur.copy(emptyRaw = avg) else cur.copy(fullRaw = avg)
        viewModelScope.launch {
            repo.saveCalibration(next)
            _ui.update { s ->
                val r = s.reading?.let { old ->
                    val p = next.percent(old.raw)
                    old.copy(
                        percent = p,
                        litres = if (p != null && old.capacityLitres != null) p / 100f * old.capacityLitres else null,
                    )
                }
                s.copy(calibration = next, reading = r)
            }
        }
    }

    override fun onCleared() {
        repo.close()
    }

    private companion object {
        const val VERIFY_TIMEOUT_MS = 8_000L
        const val RETRY_MS = 3_000L
    }
}
