package com.aura.what2eat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = RawSurfaceLight,
    primaryContainer = OrangeGradientEnd,
    secondary = SecondaryEmerald,
    onSecondary = RawSurfaceLight,
    secondaryContainer = EmeraldGradientEnd,
    background = RawBackgroundLight,
    onBackground = RawTextDark,
    surface = RawSurfaceLight,
    onSurface = RawTextDark,
    surfaceVariant = RawBackgroundLight,
    outline = RawCardBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = RawTextDark,
    primaryContainer = PrimaryDark,
    secondary = SecondaryEmerald,
    onSecondary = RawTextDark,
    background = RawBackgroundDark,
    onBackground = RawTextLight,
    surface = RawSurfaceDark,
    onSurface = RawTextLight,
    surfaceVariant = RawSurfaceDark,
    outline = RawCardBorderDark
)

@Composable
fun What2EatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
