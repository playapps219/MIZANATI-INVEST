package com.mizanati.invest.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette extracted from the original app's CSS variables.

data class Theme(
    val bg: Color, val card: Color, val card2: Color, val text: Color,
    val muted: Color, val border: Color, val green: Color, val red: Color,
    val primary: Color,
)

fun themeFor(dark: Boolean) = if (dark) Theme(
    bg = Color(0xFF0B0F17), card = Color(0xFF141A26), card2 = Color(0xFF1B2231),
    text = Color(0xFFEAF0FA), muted = Color(0xFF8B97AB), border = Color(0xFF252E40),
    green = Color(0xFF3DDC97), red = Color(0xFFFF6B70), primary = Color(0xFF8B83FF),
) else Theme(
    bg = Color(0xFFF2F4F8), card = Color(0xFFFFFFFF), card2 = Color(0xFFF1F4F9),
    text = Color(0xFF141B26), muted = Color(0xFF5B6676), border = Color(0xFFDFE4EC),
    green = Color(0xFF0E9F6E), red = Color(0xFFE5484D), primary = Color(0xFF635BFF),
)

@Composable
fun MizanatiTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val t = themeFor(darkMode)
    val scheme: ColorScheme = if (darkMode) darkColorScheme(
        primary = t.primary, background = t.bg, surface = t.card,
        onBackground = t.text, onSurface = t.text,
        error = t.red, secondary = t.green,
    ) else lightColorScheme(
        primary = t.primary, background = t.bg, surface = t.card,
        onBackground = t.text, onSurface = t.text,
        error = t.red, secondary = t.green,
    )
    LocalTheme = t
    MaterialTheme(colorScheme = scheme, content = content)
}

lateinit var LocalTheme: Theme
