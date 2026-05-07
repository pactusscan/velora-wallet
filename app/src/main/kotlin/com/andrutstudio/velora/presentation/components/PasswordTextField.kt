package com.andrutstudio.velora.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

enum class PasswordStrength(val label: String, val fraction: Float) {
    WEAK("Weak", 0.25f),
    FAIR("Fair", 0.5f),
    STRONG("Strong", 0.75f),
    VERY_STRONG("Very strong", 1f),
}

fun evaluateStrength(password: String): PasswordStrength? {
    if (password.isEmpty()) return null
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when (score) {
        0, 1 -> PasswordStrength.WEAK
        2    -> PasswordStrength.FAIR
        3    -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    showStrengthMeter: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    val strength = if (showStrengthMeter) evaluateStrength(value) else null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                    )
                }
            },
            supportingText = supportingText?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showStrengthMeter && strength != null) {
            Spacer(Modifier.height(4.dp))
            StrengthMeter(strength = strength)
        }
    }
}

@Composable
private fun StrengthMeter(strength: PasswordStrength) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK        -> Color(0xFFEF5350)
            PasswordStrength.FAIR        -> Color(0xFFFF9800)
            PasswordStrength.STRONG      -> Color(0xFFFFEB3B)
            PasswordStrength.VERY_STRONG -> Color(0xFF00C896)
        },
        label = "strength_color",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(4) { i ->
            val filled = i < (strength.fraction * 4).toInt().coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { if (filled) 1f else 0f },
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp),
                color = if (filled) color else MaterialTheme.colorScheme.outlineVariant,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
    Spacer(Modifier.height(2.dp))
    Text(
        text = strength.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
