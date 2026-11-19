package com.fardeen.waterdashboard.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fardeen.waterdashboard.model.Calibration
import com.fardeen.waterdashboard.model.WaterReading
import com.fardeen.waterdashboard.ui.components.AquaLogo
import com.fardeen.waterdashboard.ui.theme.Aqua
import kotlin.math.roundToInt

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E2238),
        titleContentColor = Aqua.TextHi,
        textContentColor = Aqua.TextMid,
        icon = { AquaLogo(56.dp) },
        title = { Text("Aqua Core · v1.0", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Smart water intelligence for an Arduino water level sensor connected over USB OTG.\n\n" +
                    "Every number and every point on the graph is a real reading received from your sensor. " +
                    "The app never generates demo data. The level percentage appears only after you " +
                    "calibrate EMPTY and FULL.\n\n" +
                    "Arduino output, one line per reading:\n{\"raw\":312.4}",
                fontSize = 13.sp,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = Aqua.Cyan) } },
    )
}

@Composable
fun CalibrationDialog(
    reading: WaterReading?,
    calibration: Calibration,
    onSetEmpty: () -> Unit,
    onSetFull: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E2238),
        titleContentColor = Aqua.TextHi,
        textContentColor = Aqua.TextMid,
        title = { Text("Calibrate sensor", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "1. Keep the sensor dry (empty tank) and tap SET EMPTY.\n" +
                        "2. Put the sensor at the full level and tap SET FULL.",
                    fontSize = 13.sp,
                )
                Row(
                    Modifier.fillMaxWidth().border(1.dp, Color(0x4D2FE6FF), RoundedCornerShape(12.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("LIVE SENSOR VALUE", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        reading?.raw?.roundToInt()?.toString() ?: "—",
                        color = Aqua.Cyan, fontSize = 22.sp, fontWeight = FontWeight.Light,
                    )
                }
                PointRow("EMPTY", calibration.emptyRaw, "SET EMPTY", onSetEmpty, reading != null)
                PointRow("FULL", calibration.fullRaw, "SET FULL", onSetFull, reading != null)
                if (calibration.emptyRaw != null && calibration.fullRaw != null && !calibration.isValid) {
                    Text(
                        "EMPTY and FULL are too close (need at least ${Calibration.MIN_SPAN.roundToInt()} apart). " +
                            "Change the water level and set the point again.",
                        color = Aqua.Warn, fontSize = 12.sp,
                    )
                }
                Text("Changing the calibration clears the saved history.", fontSize = 11.sp, color = Aqua.TextLow)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("DONE", color = Aqua.Cyan) } },
    )
}

@Composable
private fun PointRow(name: String, value: Float?, button: String, onClick: () -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(
                value?.roundToInt()?.toString() ?: "not set",
                color = if (value != null) Aqua.TextHi else Aqua.TextLow, fontSize = 18.sp,
            )
        }
        TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(start = 8.dp)) {
            Text(button, color = if (enabled) Aqua.Cyan else Aqua.TextLow, fontWeight = FontWeight.Bold)
        }
    }
}
