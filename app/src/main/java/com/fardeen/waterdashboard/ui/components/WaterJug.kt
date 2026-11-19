package com.fardeen.waterdashboard.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import com.fardeen.waterdashboard.ui.theme.Aqua
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Transparent glass jug. The water height follows [fraction] (0..1) of the real tank level.
 * [label] is the big text inside the glass (e.g. "78" or "--").
 */
@Composable
fun WaterJug(fraction: Float, label: String, modifier: Modifier = Modifier) {
    val fill = animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "fill",
    )
    val t = rememberInfiniteTransition(label = "water")
    val twoPi = (2 * PI).toFloat()
    val p1 = t.animateFloat(0f, twoPi, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "w1")
    val p2 = t.animateFloat(twoPi, 0f, infiniteRepeatable(tween(4700, easing = LinearEasing)), label = "w2")
    val rise = t.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "r")
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        drawJug(fill.value, p1.value, p2.value, rise.value, measurer, label)
    }
}

private fun bodyPath(l: Float, t: Float, r: Float, b: Float, inset: Float, rad: Float): Path = Path().apply {
    moveTo(l, t)
    lineTo(r, t)
    lineTo(r - inset, b - rad)
    quadraticBezierTo(r - inset, b, r - inset - rad, b)
    lineTo(l + inset + rad, b)
    quadraticBezierTo(l + inset, b, l + inset, b - rad)
    close()
}

private val BUBBLES = listOf(
    Triple(0.22f, 0.9f, 0.00f), Triple(0.38f, 1.3f, 0.35f), Triple(0.50f, 0.8f, 0.70f),
    Triple(0.63f, 1.1f, 0.15f), Triple(0.76f, 1.4f, 0.55f), Triple(0.45f, 1.0f, 0.90f),
)

private fun DrawScope.drawJug(
    fill: Float, p1: Float, p2: Float, rise: Float,
    measurer: TextMeasurer, label: String,
) {
    val w = size.width
    val h = size.height
    val bodyW = min(w * 0.70f, h * 0.92f * 0.80f)
    val handleW = bodyW * 0.20f
    val l = (w - (bodyW + handleW)) / 2f
    val r = l + bodyW
    val cx = (l + r) / 2f
    val rimH = bodyW * 0.16f
    val top = h * 0.03f
    val y0 = top + rimH / 2f
    val b = h * 0.95f
    val inset = bodyW * 0.07f
    val rad = bodyW * 0.16f
    val wall = bodyW * 0.04f
    val bodyH = b - y0

    val body = bodyPath(l, y0, r, b, inset, rad)
    val innerBottom = b - wall
    val inner = bodyPath(l + wall, y0, r - wall, innerBottom, inset, rad - wall)
    val innerH = innerBottom - y0
    val usable = innerH * 0.94f

    // Glow on the "table" under the jug.
    drawOval(
        brush = Brush.radialGradient(
            listOf(Aqua.Cyan.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, b), radius = bodyW * 0.7f,
        ),
        topLeft = Offset(cx - bodyW * 0.7f, b - rimH * 0.6f),
        size = Size(bodyW * 1.4f, rimH * 1.2f),
    )

    // Handle (behind the body).
    val hy1 = y0 + bodyH * 0.12f
    val hy2 = y0 + bodyH * 0.62f
    val handle = Path().apply {
        moveTo(r - wall, hy1)
        cubicTo(r + handleW * 1.25f, hy1 - bodyH * 0.06f, r + handleW * 1.3f, hy2 + bodyH * 0.04f, r - inset * 0.55f - wall, hy2)
    }
    drawPath(
        handle,
        Brush.linearGradient(listOf(Color(0x80FFFFFF), Color(0x4D2FE6FF)), Offset(r, hy1), Offset(r + handleW, hy2)),
        style = Stroke(width = wall * 2.6f, cap = StrokeCap.Round),
    )
    drawPath(handle, Color(0x2EFFFFFF), style = Stroke(width = wall * 0.8f, cap = StrokeCap.Round))

    // Glass body.
    drawPath(
        body,
        Brush.horizontalGradient(listOf(Color(0x21FFFFFF), Color(0x0AFFFFFF), Color(0x17FFFFFF)), startX = l, endX = r),
    )

    // Water.
    val water = fill.coerceIn(0f, 1f)
    val waterY = innerBottom - water * usable
    clipPath(inner) {
        if (water > 0.003f) {
            val amp = bodyW * 0.022f * min(1f, water * 12f)
            val step = bodyW / 40f

            fun wave(phase: Float, freq: Float, closed: Boolean): Path = Path().apply {
                var x = l - step
                if (closed) {
                    moveTo(x, innerBottom + 4f)
                    lineTo(x, waterY + sin(x / bodyW * freq * 2f * PI.toFloat() + phase) * amp)
                } else {
                    moveTo(x, waterY + sin(x / bodyW * freq * 2f * PI.toFloat() + phase) * amp)
                }
                while (x <= r + step) {
                    x += step
                    lineTo(x, waterY + sin(x / bodyW * freq * 2f * PI.toFloat() + phase) * amp)
                }
                if (closed) {
                    lineTo(x, innerBottom + 4f)
                    close()
                }
            }

            drawPath(
                wave(p2, 1.1f, closed = true),
                Brush.verticalGradient(listOf(Color(0x8033E0FF), Color(0xB01A5FD8)), startY = waterY - amp, endY = innerBottom),
            )
            drawPath(
                wave(p1, 1.4f, closed = true),
                Brush.verticalGradient(listOf(Color(0xCC35DDF8), Color(0xF21456C8)), startY = waterY - amp, endY = innerBottom),
            )
            drawPath(wave(p1, 1.4f, closed = false), Color(0x99FFFFFF), style = Stroke(width = 1.6f))

            // Light falling through the water.
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0x1AFFFFFF), Color.Transparent), startX = l, endX = r,
                ),
                topLeft = Offset(l, waterY),
                size = Size(bodyW, innerBottom - waterY),
            )

            // Rising bubbles.
            val waterH = innerBottom - waterY
            if (waterH > bodyW * 0.15f) {
                BUBBLES.forEach { (xf, speed, off) ->
                    val prog = (rise * speed + off) % 1f
                    val by = innerBottom - prog * waterH
                    val bx = l + bodyW * xf + sin(prog * 9f + off * 6f) * bodyW * 0.015f
                    drawCircle(Color(0x59FFFFFF), radius = bodyW * 0.014f + off * 2f, center = Offset(bx, by), style = Stroke(1.2f))
                }
            }
        }

        // Graduation marks (25 / 50 / 75 / 100 %).
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { f ->
            val y = innerBottom - f * usable
            val len = if (f == 0.5f) bodyW * 0.09f else bodyW * 0.055f
            drawLine(Color(0x59FFFFFF), Offset(r - wall - len, y), Offset(r - wall - 1f, y), strokeWidth = 1.5f)
        }
    }

    // Glass reflections.
    clipPath(body) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x0DFFFFFF)), startY = y0, endY = b),
            topLeft = Offset(l + bodyW * 0.09f, y0 + rimH * 0.9f),
            size = Size(bodyW * 0.07f, bodyH * 0.72f),
            cornerRadius = CornerRadius(bodyW * 0.035f),
        )
        drawRoundRect(
            Color(0x2EFFFFFF),
            topLeft = Offset(r - bodyW * 0.15f, y0 + rimH * 1.2f),
            size = Size(bodyW * 0.022f, bodyH * 0.5f),
            cornerRadius = CornerRadius(bodyW * 0.011f),
        )
        drawCircle(Color(0x80FFFFFF), radius = bodyW * 0.012f, center = Offset(l + bodyW * 0.125f, y0 + bodyH * 0.82f))
    }

    // Outline + rim.
    drawPath(
        body,
        Brush.horizontalGradient(listOf(Color(0xBFFFFFFF), Color(0x802FE6FF), Color(0x592F7BFF)), startX = l, endX = r),
        style = Stroke(width = (bodyW * 0.012f).coerceAtLeast(1.5f)),
    )
    drawOval(Color(0x17FFFFFF), topLeft = Offset(l, top), size = Size(bodyW, rimH))
    drawOval(Color(0x99BFF6FF), topLeft = Offset(l, top), size = Size(bodyW, rimH), style = Stroke(width = 1.6f))

    // Big percentage inside the glass.
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontSize = (bodyW * 0.30f).toSp(), fontWeight = FontWeight.Light, color = Aqua.TextHi)) {
            append(label)
        }
        if (label.firstOrNull()?.isDigit() == true) {
            withStyle(SpanStyle(fontSize = (bodyW * 0.12f).toSp(), color = Aqua.CyanSoft)) { append("%") }
        }
    }
    val layout = measurer.measure(text, TextStyle(shadow = Shadow(Color(0xAA00101F), Offset(0f, 3f), 14f)))
    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, y0 + innerH * 0.46f - layout.size.height / 2f))
}
