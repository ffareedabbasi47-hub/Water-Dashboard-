package com.fardeen.waterdashboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import com.fardeen.waterdashboard.ui.theme.Aqua

/** The Aqua Core mark: a glowing water drop inside a thin ring (same geometry as the launcher icon). */
@Composable
fun AquaLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 108f
        scale(s, s, pivot = Offset.Zero) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Aqua.Cyan.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(54f, 60f), radius = 54f,
                ),
                radius = 54f, center = Offset(54f, 54f),
            )
            drawCircle(
                Aqua.Cyan.copy(alpha = 0.45f), radius = 31f, center = Offset(54f, 54f),
                style = Stroke(width = 1.5f),
            )
            val drop = Path().apply {
                moveTo(54f, 26f)
                cubicTo(54f, 26f, 35f, 48f, 35f, 61f)
                cubicTo(35f, 71.5f, 43.5f, 80f, 54f, 80f)
                cubicTo(64.5f, 80f, 73f, 71.5f, 73f, 61f)
                cubicTo(73f, 48f, 54f, 26f, 54f, 26f)
                close()
            }
            drawPath(
                drop,
                Brush.verticalGradient(listOf(Aqua.CyanSoft, Color(0xFF1F6BFF)), startY = 26f, endY = 80f),
            )
            val hi = Path().apply {
                moveTo(44f, 60f)
                cubicTo(44f, 66f, 47.5f, 71f, 53f, 73f)
            }
            drawPath(hi, Color(0xB3FFFFFF), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
            val wave = Path().apply {
                moveTo(41f, 64f)
                quadraticBezierTo(47.5f, 59f, 54f, 64f)
                quadraticBezierTo(60.5f, 69f, 67f, 64f)
            }
            drawPath(wave, Color(0x99FFFFFF), style = Stroke(width = 2f, cap = StrokeCap.Round))
        }
    }
}
