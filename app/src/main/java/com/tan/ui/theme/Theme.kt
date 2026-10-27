package com.tan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TanTurquesa,
    onPrimary = Color.White,
    secondary = TanNaranja,
    onSecondary = Color.White,
    background = TanFondo,
    onBackground = TanTexto,
    surface = Color.White,
    onSurface = TanTexto
)

@Composable
fun TANTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}