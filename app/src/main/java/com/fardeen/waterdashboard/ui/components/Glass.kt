package com.fardeen.waterdashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fardeen.waterdashboard.ui.theme.Aqua

/** Translucent glassmorphism surface with a soft cyan glow and a gradient hairline border. */
fun Modifier.glass(radius: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(
            elevation = 14.dp,
            shape = shape,
            ambientColor = Aqua.Cyan.copy(alpha = 0.35f),
            spotColor = Aqua.Cyan.copy(alpha = 0.35f),
        )
        .clip(shape)
        .background(
            Brush.linearGradient(listOf(Color(0x2E5BD6FF), Color(0x0F2F7BFF), Color(0x1F0A2540)))
        )
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color(0x8056E8FF), Color(0x1A56E8FF), Color(0x4D2F7BFF))),
            shape,
        )
}

/** Full-screen navy gradient with soft cyan/blue light pools. */
@Composable
fun AquaBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Aqua.Navy2, Aqua.Navy1, Aqua.Navy0)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Aqua.Cyan.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f),
                        radius = size.width * 0.9f,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Aqua.Blue.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height),
                        radius = size.width * 1.0f,
                    )
                )
            }
    ) { content() }
}

@Composable
fun GradientButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val active = enabled || loading
    Box(
        modifier
            .height(54.dp)
            .shadow(
                if (active) 16.dp else 0.dp, shape,
                ambientColor = Aqua.Cyan, spotColor = Aqua.Cyan,
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (active) listOf(Aqua.Cyan, Aqua.Blue) else listOf(Color(0xFF1B3550), Color(0xFF1B3550))
                )
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Aqua.Navy0,
                    strokeWidth = 2.5.dp,
                )
            }
            Text(
                text,
                color = Aqua.Navy0,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize = 15.sp,
            )
        }
    }
}
