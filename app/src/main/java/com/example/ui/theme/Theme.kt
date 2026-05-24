package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = CyberPurple,
    onSecondary = Color.White,
    tertiary = CyberAccent,
    onTertiary = Color.Black,
    background = CyberBg,
    onBackground = Color.White,
    surface = CyberSurface,
    onSurface = Color.White,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = Color.White,
    error = SignalPoor,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
