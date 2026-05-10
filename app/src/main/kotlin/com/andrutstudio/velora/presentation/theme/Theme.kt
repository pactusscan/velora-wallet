package com.andrutstudio.velora.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = OnPrimary,
    primaryContainer = BrandTealDim,
    secondary = BrandPurple,
    onSecondary = OnPrimary,
    secondaryContainer = BrandPurpleDim,
    tertiary = DangerRed,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = DangerRed,
)

@Composable
fun VeloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PactusTypography,
        content = content,
    )
}
