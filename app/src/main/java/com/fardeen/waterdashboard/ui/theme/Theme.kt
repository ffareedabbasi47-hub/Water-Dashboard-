package com.fardeen.waterdashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

object Aqua {
    val Navy0 = Color(0xFF040B16)
    val Navy1 = Color(0xFF081528)
    val Navy2 = Color(0xFF0C2038)
    val Cyan = Color(0xFF2FE6FF)
    val CyanSoft = Color(0xFF8CF6FF)
    val Blue = Color(0xFF2F7BFF)
    val TextHi = Color(0xFFEAF8FF)
    val TextMid = Color(0xFF8FB0C7)
    val TextLow = Color(0xFF5C7A90)
    val Ok = Color(0xFF3DF5B0)
    val Warn = Color(0xFFFFB84D)
    val Danger = Color(0xFFFF5C7A)
}

@Composable
fun AquaTheme(content: @Composable () -> Unit) {
    // Cap the system font scale so the one-screen layout never overflows.
    val base = LocalDensity.current
    val density = Density(base.density, fontScale = base.fontScale.coerceAtMost(1.15f))
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Aqua.Cyan,
            onPrimary = Aqua.Navy0,
            background = Aqua.Navy0,
            onBackground = Aqua.TextHi,
            surface = Aqua.Navy1,
            onSurface = Aqua.TextHi,
            surfaceContainer = Color(0xFF0E2238),
            surfaceContainerHigh = Color(0xFF0E2238),
            error = Aqua.Danger,
        )
    ) {
        CompositionLocalProvider(LocalDensity provides density, content = content)
    }
}
