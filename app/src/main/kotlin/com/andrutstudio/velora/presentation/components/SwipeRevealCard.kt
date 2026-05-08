package com.andrutstudio.velora.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class RevealState { LeftRevealed, Resting, RightRevealed }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRevealCard(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onCustom: (() -> Unit)? = null,
    customIcon: ImageVector = Icons.Rounded.Edit,
    customLabel: String = "Custom",
    customColor: Color = Color(0xFF1F8FE5),
    actionWidth: androidx.compose.ui.unit.Dp = 80.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val scope = rememberCoroutineScope()
    val decaySpec = rememberSplineBasedDecay<Float>()

    val state = remember(actionWidthPx, decaySpec) {
        AnchoredDraggableState(
            initialValue = RevealState.Resting,
            anchors = DraggableAnchors {
                val leftAnchor = if (onCustom != null) actionWidthPx * 2 else actionWidthPx
                RevealState.LeftRevealed at leftAnchor
                RevealState.Resting at 0f
                RevealState.RightRevealed at -actionWidthPx
            },
            positionalThreshold = { distance -> distance * 0.4f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(280),
            decayAnimationSpec = decaySpec,
        )
    }

    fun reset() = scope.launch { state.animateTo(RevealState.Resting) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        val currentOffset = state.requireOffset()

        // Background Actions — only rendered when swiping
        if (currentOffset != 0f) {
            Row(modifier = Modifier.matchParentSize()) {
                if (currentOffset > 0) {
                    if (onCustom != null) {
                        RevealAction(
                            icon = customIcon,
                            label = customLabel,
                            container = customColor,
                            onClick = {
                                onCustom()
                                reset()
                            },
                            modifier = Modifier
                                .width(actionWidth)
                                .fillMaxHeight(),
                        )
                    }
                    RevealAction(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        container = Color(0xFF1F8FE5),
                        onClick = {
                            onEdit()
                            reset()
                        },
                        modifier = Modifier
                            .width(actionWidth)
                            .fillMaxHeight(),
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                if (currentOffset < 0) {
                    RevealAction(
                        icon = Icons.Rounded.Delete,
                        label = "Delete",
                        container = Color(0xFFE53935),
                        onClick = {
                            onDelete()
                            reset()
                        },
                        modifier = Modifier
                            .width(actionWidth)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(currentOffset.roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal),
        ) {
            content()
        }
    }
}

@Composable
private fun RevealAction(
    icon: ImageVector,
    label: String,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(container)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SwipeRevealCardPreview() {
    VeloraTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SwipeRevealCard(
                onEdit = {},
                onDelete = {},
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Swipe me left or right")
                    }
                }
            )
        }
    }
}
