package com.fardeen.waterdashboard.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fardeen.waterdashboard.model.HistoryPoint
import com.fardeen.waterdashboard.model.LinkState
import com.fardeen.waterdashboard.ui.components.AquaLogo
import com.fardeen.waterdashboard.ui.components.HistoryChart
import com.fardeen.waterdashboard.ui.components.WaterJug
import com.fardeen.waterdashboard.ui.components.glass
import com.fardeen.waterdashboard.ui.theme.Aqua
import com.fardeen.waterdashboard.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Screen 2: fixed one-screen dashboard (no scrolling anywhere). */
@Composable
fun DashboardScreen(
    ui: UiState,
    history: List<HistoryPoint>,
    onSettings: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onAbout: () -> Unit,
    onCalibrate: () -> Unit,
) {
    val reading = ui.reading
    val online = ui.link == LinkState.Online

    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
        val k = (maxHeight / 740.dp).coerceIn(0.8f, 1.1f)

        Column(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBar(onSettings, onReconnect, onDisconnect, onAbout)
            StatusRow(ui)

            Row(
                Modifier.fillMaxWidth().weight(1.35f).glass(26.dp).padding(8.dp).alpha(if (online) 1f else 0.75f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaterJug(
                    fraction = (reading?.percent ?: 0f) / 100f,
                    label = reading?.percent?.let { it.roundToInt().toString() } ?: "--",
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
                Stats(ui, k, onCalibrate, Modifier.weight(0.95f).fillMaxHeight().padding(start = 4.dp, end = 10.dp, top = 10.dp, bottom = 10.dp))
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).glass(26.dp).padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("WATER LEVEL HISTORY", color = Aqua.TextMid, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    val (c, t) = if (online) Aqua.Ok to "LIVE · REAL SENSOR DATA" else Aqua.Warn to "OFFLINE · LAST READINGS"
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c))
                    Spacer(Modifier.width(5.dp))
                    Text(t, color = c, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(4.dp))
                HistoryChart(history, Modifier.fillMaxWidth().weight(1f).alpha(if (online) 1f else 0.6f))
            }
        }
    }
}

@Composable
private fun TopBar(onSettings: () -> Unit, onReconnect: () -> Unit, onDisconnect: () -> Unit, onAbout: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        AquaLogo(40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("AQUA CORE", color = Aqua.Cyan, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text("WATER INTELLIGENCE", color = Aqua.TextMid, fontSize = 9.sp, letterSpacing = 3.sp)
        }
        Box {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Default.Menu, "Menu", tint = Aqua.TextHi)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text("Connection Settings") },
                    leadingIcon = { Icon(Icons.Default.Settings, null, tint = Aqua.Cyan) },
                    onClick = { open = false; onSettings() },
                )
                DropdownMenuItem(
                    text = { Text("Reconnect") },
                    leadingIcon = { Icon(Icons.Default.Refresh, null, tint = Aqua.Cyan) },
                    onClick = { open = false; onReconnect() },
                )
                DropdownMenuItem(
                    text = { Text("Disconnect") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Aqua.Cyan) },
                    onClick = { open = false; onDisconnect() },
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    leadingIcon = { Icon(Icons.Default.Info, null, tint = Aqua.Cyan) },
                    onClick = { open = false; onAbout() },
                )
            }
        }
    }
}

@Composable
private fun StatusRow(ui: UiState) {
    val (color, label) = when (ui.link) {
        LinkState.Online -> Aqua.Ok to "LIVE"
        LinkState.Reconnecting -> Aqua.Warn to "RECONNECTING"
        is LinkState.Lost -> Aqua.Danger to "NO SIGNAL · RETRYING"
        LinkState.Offline -> Aqua.TextLow to "OFFLINE"
    }
    val pulse = rememberInfiniteTransition(label = "dot")
        .animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = if (ui.link == LinkState.Online) pulse.value else 1f)))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        val info = when (val l = ui.link) {
            is LinkState.Lost -> l.message
            else -> listOfNotNull(ui.deviceLabel, ui.reading?.let { timeFmt.format(Date(it.timestamp)) }).joinToString("  •  ")
        }
        Text(
            info, color = Aqua.TextLow, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Stats(ui: UiState, k: Float, onCalibrate: () -> Unit, modifier: Modifier) {
    val r = ui.reading
    val calibrated = ui.calibration.isValid
    Column(modifier, verticalArrangement = Arrangement.SpaceEvenly) {
        StatBlock("AVAILABLE", r?.litres?.let(::fmtLitres), "L", big = true, k = k)
        HorizontalDivider(color = Aqua.Cyan.copy(alpha = 0.15f))
        StatBlock("TOTAL CAPACITY", r?.capacityLitres?.let(::fmtLitres), "L", big = false, k = k)
        HorizontalDivider(color = Aqua.Cyan.copy(alpha = 0.15f))
        StatBlock("SENSOR RAW", r?.raw?.roundToInt()?.toString(), "/1023", big = false, k = k)
        if (calibrated) {
            val p = r?.percent ?: 0f
            val (c, t) = when {
                p < 15f -> Aqua.Danger to "CRITICAL"
                p < 35f -> Aqua.Warn to "LOW"
                p >= 95f -> Aqua.Cyan to "FULL"
                else -> Aqua.Ok to "NORMAL"
            }
            Text(
                t, color = c, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.clip(CircleShape).background(c.copy(alpha = 0.14f)).border(1.dp, c.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        } else {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Aqua.Warn.copy(alpha = 0.14f))
                    .border(1.dp, Aqua.Warn.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onCalibrate).padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Build, null, tint = Aqua.Warn, modifier = Modifier.size(14.dp))
                Text("CALIBRATE SENSOR", color = Aqua.Warn, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String?, unit: String, big: Boolean, k: Float) {
    Column {
        Text(label, color = Aqua.TextMid, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value ?: "—", color = Aqua.TextHi, fontSize = ((if (big) 30 else 20) * k).sp,
                fontWeight = FontWeight.Light, maxLines = 1,
            )
            Spacer(Modifier.width(4.dp))
            Text(unit, color = Aqua.Cyan, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp), maxLines = 1)
        }
    }
}

private fun fmtLitres(v: Float): String =
    if (v < 100f) String.format(Locale.getDefault(), "%.1f", v) else String.format(Locale.getDefault(), "%,.0f", v)
