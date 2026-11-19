package com.fardeen.waterdashboard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.fardeen.waterdashboard.model.HistoryPoint
import com.fardeen.waterdashboard.ui.theme.Aqua
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val SLOTS = 40

/** Line graph of real, calibrated water-level readings. New readings slide in smoothly. */
@Composable
fun HistoryChart(points: List<HistoryPoint>, modifier: Modifier = Modifier) {
    val visible = remember(points) { points.takeLast(SLOTS + 1) }
    val labels = remember(visible) { timeLabels(visible) }
    val measurer = rememberTextMeasurer()

    val range = remember(visible) { yRange(visible) }
    val lo = animateFloatAsState(range.first, tween(700), label = "lo")
    val hi = animateFloatAsState(range.second, tween(700), label = "hi")

    val slide = remember { Animatable(0f) }
    val newestKey = visible.lastOrNull()?.timestamp
    LaunchedEffect(newestKey) {
        if (newestKey != null) {
            slide.snapTo(-1f)
            slide.animateTo(0f, tween(750, easing = FastOutSlowInEasing))
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p")

    Canvas(modifier) {
        val pl = 36.dp.toPx()
        val pr = size.width - 12.dp.toPx()
        val pt = 8.dp.toPx()
        val pb = size.height - 20.dp.toPx()
        val pw = pr - pl
        val ph = pb - pt
        val loV = lo.value
        val hiV = hi.value
        val axis = TextStyle(color = Aqua.TextLow, fontSize = 9.sp)

        fun yOf(v: Float) = pb - ((v - loV) / (hiV - loV)).coerceIn(-0.05f, 1.05f) * ph

        // Grid + % labels.
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
        for (i in 0..2) {
            val v = loV + (hiV - loV) * i / 2f
            val y = yOf(v)
            drawLine(Color(0x1F7DF3FF), Offset(pl, y), Offset(pr, y), strokeWidth = 1f, pathEffect = dash)
            val t = measurer.measure("${v.roundToInt()}%", axis)
            drawText(t, topLeft = Offset(pl - t.size.width - 6.dp.toPx(), y - t.size.height / 2f))
        }

        val m = visible.size
        if (m < 2) {
            val t = measurer.measure("Collecting readings…", TextStyle(color = Aqua.TextMid, fontSize = 11.sp))
            drawText(t, topLeft = Offset((size.width - t.size.width) / 2f, (size.height - t.size.height) / 2f))
            return@Canvas
        }

        val slot = pw / (SLOTS - 1)
        val s = slide.value
        val progress = (s + 1f).coerceIn(0f, 1f)
        val xs = FloatArray(m)
        val ys = FloatArray(m)
        for (i in 0 until m) {
            val d = m - 1 - i
            xs[i] = if (d == 0) pr else pr - (d + s) * slot
            var v = visible[i].percent
            if (d == 0) v = lerp(visible[m - 2].percent, v, progress)
            ys[i] = yOf(v)
        }

        fun smooth(closed: Boolean) = Path().apply {
            moveTo(xs[0], if (closed) pb else ys[0])
            if (closed) lineTo(xs[0], ys[0])
            for (i in 1 until m) {
                val cx = (xs[i - 1] + xs[i]) / 2f
                cubicTo(cx, ys[i - 1], cx, ys[i], xs[i], ys[i])
            }
            if (closed) {
                lineTo(xs[m - 1], pb)
                close()
            }
        }

        clipRect(left = pl, top = pt - 4.dp.toPx(), right = pr + 1f, bottom = pb) {
            drawPath(
                smooth(true),
                Brush.verticalGradient(listOf(Aqua.Cyan.copy(alpha = 0.38f), Color.Transparent), startY = pt, endY = pb),
            )
            val line = smooth(false)
            drawPath(line, Aqua.Cyan.copy(alpha = 0.25f), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(line, Aqua.Cyan, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Newest reading marker.
        val c = Offset(pr, ys[m - 1])
        drawCircle(Aqua.Cyan.copy(alpha = 0.35f * (1f - pulse.value)), radius = (4f + 8f * pulse.value).dp.toPx(), center = c)
        drawCircle(Aqua.Cyan, radius = 4.dp.toPx(), center = c)
        drawCircle(Color.White, radius = 1.8.dp.toPx(), center = c)

        // Time labels, anchored to real readings.
        labels.forEach { (idx, text) ->
            val x = pr - (m - 1 - idx) * slot
            val t = measurer.measure(text, axis)
            val left = (x - t.size.width / 2f).coerceIn(pl - 4.dp.toPx(), max(pl, size.width - t.size.width))
            drawText(t, topLeft = Offset(left, pb + 5.dp.toPx()))
        }
    }
}

private fun yRange(p: List<HistoryPoint>): Pair<Float, Float> {
    if (p.isEmpty()) return 0f to 100f
    val mn = p.minOf { it.percent }
    val mx = p.maxOf { it.percent }
    val pad = max((mx - mn) * 0.25f, 3f)
    var lo = mn - pad
    var hi = mx + pad
    if (hi - lo < 12f) {
        val mid = (hi + lo) / 2f
        lo = mid - 6f
        hi = mid + 6f
    }
    if (lo < 0f) { hi -= lo; lo = 0f }
    if (hi > 100f) { lo -= hi - 100f; hi = 100f }
    return max(lo, 0f) to min(hi, 100f)
}

private fun timeLabels(p: List<HistoryPoint>): List<Pair<Int, String>> {
    val m = p.size
    if (m == 0) return emptyList()
    val first = max(0, m - SLOTS)
    val span = p[m - 1].timestamp - p[first].timestamp
    val pattern = when {
        span <= 15 * 60_000L -> "HH:mm:ss"
        span <= 36 * 3_600_000L -> "HH:mm"
        else -> "dd MMM"
    }
    val fmt = SimpleDateFormat(pattern, Locale.getDefault())
    val count = m - first
    return (0..3).map { k -> first + ((k * (count - 1)) / 3f).roundToInt() }
        .distinct()
        .map { idx -> idx to fmt.format(Date(p[idx].timestamp)) }
}
