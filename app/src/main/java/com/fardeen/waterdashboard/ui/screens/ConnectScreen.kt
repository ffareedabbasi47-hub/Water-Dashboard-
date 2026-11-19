package com.fardeen.waterdashboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fardeen.waterdashboard.model.AttemptState
import com.fardeen.waterdashboard.model.ConnectionForm
import com.fardeen.waterdashboard.ui.components.AquaLogo
import com.fardeen.waterdashboard.ui.components.GradientButton
import com.fardeen.waterdashboard.ui.components.glass
import com.fardeen.waterdashboard.ui.theme.Aqua

/** Screen 1: Aqua Core branding + connect form. Also reused (with a back arrow) for Connection Settings. */
@Composable
fun ConnectScreen(
    form: ConnectionForm,
    attempt: AttemptState,
    onFormChange: (ConnectionForm) -> Unit,
    onConnect: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val connecting = attempt is AttemptState.Connecting
    val focus = LocalFocusManager.current

    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        val compact = maxHeight < 560.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (onBack != null) {
                Row(Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack, enabled = !connecting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Aqua.TextHi)
                    }
                }
            }
            Spacer(Modifier.weight(1f))

            if (compact) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AquaLogo(40.dp)
                    Column {
                        Text("AQUA CORE", color = Aqua.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                        Text("WATER INTELLIGENCE", color = Aqua.TextMid, fontSize = 9.sp, letterSpacing = 3.sp)
                    }
                }
            } else {
                AquaLogo(112.dp)
                Spacer(Modifier.height(10.dp))
                Text("AQUA CORE", color = Aqua.Cyan, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                Text("WATER INTELLIGENCE", color = Aqua.TextMid, fontSize = 11.sp, letterSpacing = 5.sp)
            }
            Spacer(Modifier.height(if (compact) 12.dp else 28.dp))

            Column(
                Modifier.fillMaxWidth().glass(24.dp).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("CONNECT DEVICE", color = Aqua.TextHi, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                if (!compact) {
                    Text(
                        "Plug your Arduino into the phone with a USB OTG cable.",
                        color = Aqua.TextMid, fontSize = 12.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AquaField(
                        value = form.baud,
                        onChange = { onFormChange(form.copy(baud = it.filter { c -> c.isDigit() })) },
                        label = "Baud rate",
                        enabled = !connecting,
                        keyboard = KeyboardType.Number,
                        ime = ImeAction.Next,
                        onNext = { focus.moveFocus(FocusDirection.Next) },
                        modifier = Modifier.weight(1f),
                    )
                    AquaField(
                        value = form.capacity,
                        onChange = { onFormChange(form.copy(capacity = it.filter { c -> c.isDigit() || c == '.' || c == ',' })) },
                        label = "Tank capacity (L)",
                        enabled = !connecting,
                        keyboard = KeyboardType.Decimal,
                        ime = ImeAction.Done,
                        onNext = { focus.clearFocus(); onConnect() },
                        modifier = Modifier.weight(1.3f),
                    )
                }
                GradientButton(
                    text = if (connecting) "CONNECTING…" else "CONNECT DEVICE",
                    enabled = !connecting,
                    loading = connecting,
                    onClick = { focus.clearFocus(); onConnect() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    when (attempt) {
                        is AttemptState.Connecting -> {
                            LinearProgressIndicator(
                                Modifier.fillMaxWidth().height(3.dp),
                                color = Aqua.Cyan,
                                trackColor = Color(0x331F6BFF),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(attempt.phase, color = Aqua.TextMid, fontSize = 12.sp)
                        }
                        is AttemptState.Failed -> Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Aqua.Danger, modifier = Modifier.size(18.dp))
                            Text(attempt.message, color = Aqua.Danger, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        AttemptState.Idle -> Unit
                    }
                }
            }

            Spacer(Modifier.weight(1.2f))
            if (!compact) {
                Text(
                    "Android will ask for USB access the first time.\nNo internet, no cloud — data comes straight from your sensor.",
                    color = Aqua.TextLow, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun AquaField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboard: KeyboardType,
    ime: ImeAction,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        enabled = enabled,
        textStyle = TextStyle(fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ime),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Aqua.TextHi,
            unfocusedTextColor = Aqua.TextHi,
            disabledTextColor = Aqua.TextMid,
            cursorColor = Aqua.Cyan,
            focusedBorderColor = Aqua.Cyan,
            unfocusedBorderColor = Color(0x4D8CF6FF),
            disabledBorderColor = Color(0x268CF6FF),
            focusedLabelColor = Aqua.Cyan,
            unfocusedLabelColor = Aqua.TextMid,
            disabledLabelColor = Aqua.TextLow,
            focusedContainerColor = Color(0x14FFFFFF),
            unfocusedContainerColor = Color(0x0DFFFFFF),
            disabledContainerColor = Color(0x08FFFFFF),
        ),
        modifier = modifier,
    )
}
