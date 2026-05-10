package com.andrutstudio.velora.presentation.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand colors (from primary gradient)
val BrandTeal        = Color(0xFF23C4B2)
val BrandTealDim     = Color(0xFF1BA898)
val BrandPurple      = Color(0xFF7B78FF)
val BrandPurpleDim   = Color(0xFF6562E0)
val DangerRed        = Color(0xFFBE4859)

// Primary gradient: 135° teal → purple (top-left to bottom-right)
val BrandGradient = Brush.linearGradient(
    colors = listOf(BrandTeal, BrandPurple),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
)

// ── Dark theme ─────────────────────────────────────────────────────────────
val Background             = Color(0xFF0B0E1E)
val Surface                = Color(0xFF161B30)
val SurfaceVariant         = Color(0xFF1E2445)
val SurfaceContainer       = Color(0xFF161B30)  // cards use the same container color
val SurfaceContainerLowest = Color(0xFF0D1122)
val Outline                = Color(0xFF2A3558)
val OutlineVariant         = Color(0xFF1A2240)

val OnBackground           = Color(0xFFFFFFFF)  // Text High Emphasis
val OnSurface              = Color(0xFFFFFFFF)  // Text High Emphasis on surface
val OnSurfaceVariant       = Color(0xFF94A3B8)  // Text Low Emphasis
val OnPrimary              = Color(0xFFFFFFFF)

// ── Light theme ────────────────────────────────────────────────────────────
val BackgroundLight             = Color(0xFFF8FAFC)
val SurfaceLight                = Color(0xFFFFFFFF)
val SurfaceVariantLight         = Color(0xFFE8ECFF)
val SurfaceContainerLight       = Color(0xFFFFFFFF)  // white containers
val SurfaceContainerLowestLight = Color(0xFFF8FAFC)
val OutlineLight                = Color(0xFFCBD5E1)
val OutlineVariantLight         = Color(0xFFE2E8F0)

val OnBackgroundLight           = Color(0xFF0B0E1E)  // Text High Emphasis (inverted bg)
val OnSurfaceLight              = Color(0xFF0B0E1E)
val OnSurfaceVariantLight       = Color(0xFF64748B)  // Text Low Emphasis (darker for contrast)
