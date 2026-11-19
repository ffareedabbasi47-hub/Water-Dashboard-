package com.fardeen.waterdashboard.viewmodel

import com.fardeen.waterdashboard.model.AttemptState
import com.fardeen.waterdashboard.model.Calibration
import com.fardeen.waterdashboard.model.ConnectionForm
import com.fardeen.waterdashboard.model.LinkState
import com.fardeen.waterdashboard.model.UsbConfig
import com.fardeen.waterdashboard.model.WaterReading

enum class Screen { Connect, Dashboard }

data class UiState(
    val screen: Screen = Screen.Connect,
    val form: ConnectionForm = ConnectionForm(),
    val attempt: AttemptState = AttemptState.Idle,
    val link: LinkState = LinkState.Offline,
    val reading: WaterReading? = null,
    val activeConfig: UsbConfig? = null,
    val deviceLabel: String? = null,
    val calibration: Calibration = Calibration(),
    val showSettings: Boolean = false,
    val showCalibration: Boolean = false,
)
