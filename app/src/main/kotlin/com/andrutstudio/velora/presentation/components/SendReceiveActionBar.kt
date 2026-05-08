package com.andrutstudio.velora.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.BrandPurple

@Composable
fun SendReceiveActionBar(
    onReceive: () -> Unit,
    onSend: () -> Unit,
    onStake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            icon = Icons.Rounded.ArrowDownward,
            label = "Receive",
            background = null,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onReceive,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            icon = Icons.Rounded.VerifiedUser,
            label = "Stake",
            background = null,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onStake,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            icon = Icons.Rounded.ArrowUpward,
            label = "Send",
            background = Brush.linearGradient(
                listOf(BrandTeal.copy(alpha = 0.95f), BrandPurple.copy(alpha = 0.85f)),
            ),
            contentColor = Color.White,
            onClick = onSend,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    background: Brush?,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .clip(shape)
        .let { if (background != null) it.background(background) else it }
        .clickable(onClick = onClick)
        .padding(vertical = 14.dp)

    Row(
        modifier = base,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SendReceiveActionBarPreview() {
    VeloraTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SendReceiveActionBar(
                onReceive = {},
                onSend = {},
                onStake = {}
            )
        }
    }
}
