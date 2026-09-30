package com.aura.what2eat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Brand Core Palette (Static Constants)
val PrimaryOrange = Color(0xFFFF5E36)       // Warm Orange
val SecondaryEmerald = Color(0xFF10B981)   // Emerald Green
val PrimaryDark = Color(0xFFFF7A59)
val SecondaryDark = Color(0xFF34D399)

val RedCancel = Color(0xFFEF4444)           // Warning / Cancel / Danger Red
val SuccessGreen = Color(0xFF10B981)        // Success / Positive Green

val GoldPro = Color(0xFFF59E0B)            // Gold / Amber for PRO
val OrangeGradientStart = Color(0xFFFF5E36)
val OrangeGradientEnd = Color(0xFFFF8C42)
val EmeraldGradientStart = Color(0xFF10B981)
val EmeraldGradientEnd = Color(0xFF34D399)

// Static Base Palette for Scheme Mapping
val RawBackgroundLight = Color(0xFFF8FAFC)
val RawBackgroundDark = Color(0xFF0F172A)
val RawSurfaceLight = Color(0xFFFFFFFF)
val RawSurfaceDark = Color(0xFF1E293B)
val RawTextDark = Color(0xFF0F172A)
val RawTextLight = Color(0xFFF8FAFC)
val RawCardBorderLight = Color(0xFFE2E8F0)
val RawCardBorderDark = Color(0xFF334155)
val RawTextMutedLight = Color(0xFF64748B)
val RawTextMutedDark = Color(0xFF94A3B8)
val RawGoldProContainerLight = Color(0xFFFEF3C7)
val RawGoldProContainerDark = Color(0xFF3B2D08)

// Dynamic System-Aware Color Properties
val BackgroundOffWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) RawBackgroundDark else RawBackgroundLight

val SurfaceWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) RawSurfaceDark else RawSurfaceLight

val DarkText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) RawTextLight else RawTextDark

val CardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) RawCardBorderDark else RawCardBorderLight

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) RawTextMutedDark else RawTextMutedLight

val GoldProContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) RawGoldProContainerDark else RawGoldProContainerLight

