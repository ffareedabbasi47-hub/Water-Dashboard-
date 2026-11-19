package com.fardeen.waterdashboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fardeen.waterdashboard.ui.components.AquaBackground
import com.fardeen.waterdashboard.ui.screens.AboutDialog
import com.fardeen.waterdashboard.ui.screens.CalibrationDialog
import com.fardeen.waterdashboard.ui.screens.ConnectScreen
import com.fardeen.waterdashboard.ui.screens.DashboardScreen
import com.fardeen.waterdashboard.ui.theme.AquaTheme
import com.fardeen.waterdashboard.viewmodel.AquaViewModel
import com.fardeen.waterdashboard.viewmodel.Screen

@Composable
fun AquaApp(vm: AquaViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var about by remember { mutableStateOf(false) }

    AquaTheme {
        AquaBackground {
            val showDashboard = ui.screen == Screen.Dashboard && !ui.showSettings
            BackHandler(enabled = ui.showSettings) { vm.closeSettings() }

            Crossfade(targetState = showDashboard, label = "screen") { dashboard ->
                if (dashboard) {
                    DashboardScreen(
                        ui = ui,
                        history = history,
                        onSettings = vm::openSettings,
                        onReconnect = vm::reconnect,
                        onDisconnect = vm::disconnect,
                        onAbout = { about = true },
                        onCalibrate = vm::openCalibration,
                    )
                } else {
                    ConnectScreen(
                        form = ui.form,
                        attempt = ui.attempt,
                        onFormChange = vm::updateForm,
                        onConnect = vm::connect,
                        onBack = if (ui.showSettings) vm::closeSettings else null,
                    )
                }
            }

            if (about) AboutDialog { about = false }
            if (ui.showCalibration) {
                CalibrationDialog(
                    reading = ui.reading,
                    calibration = ui.calibration,
                    onSetEmpty = vm::setEmptyPoint,
                    onSetFull = vm::setFullPoint,
                    onDismiss = vm::closeCalibration,
                )
            }
        }
    }
}
