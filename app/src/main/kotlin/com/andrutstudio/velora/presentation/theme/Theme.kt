package com.andrutstudio.velora.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App-wide palette — derived from the Home screen design.
 * The whole app uses this single dark scheme; system theme and Material You
 * dynamic colors are intentionally ignored so the brand identity stays
 * consistent across screens.
 */
private val PactusColorScheme = darkColorScheme(
    primary = BrandTeal,
    onPrimary = OnPrimary,
    primaryContainer = BrandTealDim,
    secondary = BrandPurple,
    onSecondary = OnPrimary,
    secondaryContainer = BrandPurpleDim,
    tertiary = DangerRed,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerLowest = SurfaceContainerLowest,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = DangerRed,
)

@Composable
fun VeloraTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Light icons on dark background — matches the dark palette.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = PactusColorScheme,
        typography = PactusTypography,
        content = content,
    )
}
