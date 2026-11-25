package com.mizanati.invest.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class MzColors(
    val dark: Boolean,
    val bg: Color,
    val c: Color,
    val c2: Color,
    val t: Color,
    val m: Color,
    val b: Color,
    val a: Color,
    val r: Color,
    val p: Color,
)

val LightColors = MzColors(
    dark = false,
    bg = Color(0xFFF2F4F8),
    c = Color(0xFFFFFFFF),
    c2 = Color(0xFFEEF1F6),
    t = Color(0xFF141B26),
    m = Color(0xFF6A7686),
    b = Color(0xFFE1E6EE),
    a = Color(0xFF0E9F6E),
    r = Color(0xFFE5484D),
    p = Color(0xFF635BFF),
)

val DarkColors = MzColors(
    dark = true,
    bg = Color(0xFF0B0F17),
    c = Color(0xFF141A26),
    c2 = Color(0xFF1B2231),
    t = Color(0xFFEAF0FA),
    m = Color(0xFF8B97AB),
    b = Color(0xFF252E40),
    a = Color(0xFF3DDC97),
    r = Color(0xFFFF6B70),
    p = Color(0xFF8B83FF),
)

val LocalMz = staticCompositionLocalOf { LightColors }

fun cssGradient(angleDeg: Float, vararg stops: Pair<Float, Color>): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val len = abs(size.width * dx) + abs(size.height * dy)
        val center = Offset(size.width / 2f, size.height / 2f)
        val half = Offset(dx * len / 2f, dy * len / 2f)
        return LinearGradientShader(
            from = center - half,
            to = center + half,
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )
    }
}
