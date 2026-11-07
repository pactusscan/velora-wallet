package com.andrutstudio.velora.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Bottom-navigation icons, line style: 1.7 strokes in a 24 x 24 box, tinted by
 * the Icon. Same shapes as NavIcons.tsx in the Velora browser extension
 * (decision 2026-11-03: identical icons in both apps): Wallet = card with a
 * clasp line, Activity = pulse line, Stake = stacked layers, Node = two server
 * racks, Settings = two circles. Discover (Android only) = a compass.
 */
object NavIcons {
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y H${x + w - r} A$r,$r 0 0 1 ${x + w},${y + r} V${y + h - r} " +
            "A$r,$r 0 0 1 ${x + w - r},${y + h} H${x + r} A$r,$r 0 0 1 $x,${y + h - r} " +
            "V${y + r} A$r,$r 0 0 1 ${x + r},$y Z"

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy A$r,$r 0 1 0 ${cx + r},$cy A$r,$r 0 1 0 ${cx - r},$cy Z"

    private fun line(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.7f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Wallet: ImageVector by lazy { line("Wallet", rect(3f, 5f, 18f, 14f, 3f), "M13 12h8") }
    val Activity: ImageVector by lazy { line("Activity", "M3 12h4l3-6 4 12 3-6h4") }
    val Stake: ImageVector by lazy { line("Stake", "M12 4l9 5-9 5-9-5zM3 14l9 5 9-5") }
    val Node: ImageVector by lazy { line("Node", rect(3f, 4f, 18f, 7f, 2f), rect(3f, 13f, 18f, 7f, 2f)) }
    val Settings: ImageVector by lazy { line("Settings", circle(12f, 12f, 7f), circle(12f, 12f, 2.5f)) }
    val Discover: ImageVector by lazy { line("Discover", circle(12f, 12f, 9f), "M15.5 8.5l-2 5-5 2 2-5z") }
}
