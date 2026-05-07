package com.andrutstudio.velora.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun StepIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val active = index == currentStep
            val passed = index < currentStep

            val color by animateColorAsState(
                targetValue = when {
                    active || passed -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                label = "step_color_$index",
            )

            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (active) 24.dp else 16.dp)
                    .clip(CircleShape)
                    .background(color),
            )

            if (index < totalSteps - 1) Spacer(Modifier.width(6.dp))
        }
    }
}
