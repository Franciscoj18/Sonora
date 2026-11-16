package com.sonora.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFA2D48)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    secondaryContainer = Color(0xFF1C1C1E),
    outlineVariant = Color(0xFF38383A),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    error = Color(0xFFFF453A),
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEFEFF4),
    onSurfaceVariant = Color(0xFF8E8E93),
    secondaryContainer = Color(0xFFF2F2F7),
    outlineVariant = Color(0xFFD1D1D6),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    error = Color(0xFFFF3B30),
)

@Composable
fun SonoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
