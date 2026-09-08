package com.zyb.nativetools.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF17336F),
    primaryContainer = Color(0xFF23365F),
    onPrimaryContainer = Blue80,
    tertiary = Coral80,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Paper,
    primaryContainer = BlueContainer,
    onPrimaryContainer = Blue40,
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outlineVariant = Line,
    tertiary = Coral40,
    error = Coral40,
)

@Composable
fun NativeToolsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
