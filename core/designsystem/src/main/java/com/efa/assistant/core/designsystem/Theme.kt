package com.efa.assistant.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TealLightPrimary,
    secondary = WarmAmberLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TealDarkPrimary,
    secondary = WarmAmberDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = BorderDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = TealAmoledPrimary,
    secondary = WarmAmberDark,
    background = BackgroundAmoled,
    surface = SurfaceAmoled,
    onPrimary = BackgroundAmoled,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = BorderAmoled
)

enum class ThemeMode {
    LIGHT, DARK, AMOLED
}

@Composable
fun EFATheme(
    themeMode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
