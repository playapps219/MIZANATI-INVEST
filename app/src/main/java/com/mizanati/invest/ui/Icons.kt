package com.mizanati.invest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

object MzIcons {
    val wallet = listOf(
        "M3.5 7.5A2.5 2.5 0 0 1 6 5h12.5A2.5 2.5 0 0 1 21 7.5V18a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 18V7.5Z",
        "M3.5 8.5H18a3 3 0 0 1 3 3v2H17a2.5 2.5 0 0 0 0 5h4",
        "M17 16h.01",
    )
    val reports = listOf(
        "M4 20h16",
        "M6.5 16.5V11M12 16.5V6.5M17.5 16.5v-3.5",
        "M5 8.2l4.4-3 3.7 2.1 5.4-3.9",
    )
    val settings = listOf(
        "M4 7h9M17 7h3M4 17h3M11 17h9",
        "M13 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M7 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )
    val moon = listOf("M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z")
    val freedom = listOf(
        "M12 3a9 9 0 1 0 9 9",           // open circle (flight path)
        "M12 7a5 5 0 0 1 5 5",           // inner arc
        "M17.5 6.5 21 3M19 3h2v2",       // arrow out
    )
}

@Composable
fun SvgIcon(
    paths: List<String>,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    viewBox: Float = 24f,
    strokeWidth: Float = 2f,
    fillLastPath: Boolean = false,
) {
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.width / viewBox
        scale(s, s, pivot = Offset.Zero) {
            parsed.forEachIndexed { index, path ->
                if (fillLastPath && index == parsed.lastIndex) {
                    drawPath(path, color.copy(alpha = color.alpha * .12f))
                } else {
                    drawPath(
                        path,
                        color,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    val bars = remember { PathParser().parsePathString("M12 33V23M21 33V14M30 33V20M39 33V9").toPath() }
    val base = remember { PathParser().parsePathString("M8 40h34").toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 48f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(bars, Color.White, style = Stroke(4.2f, cap = StrokeCap.Round))
            drawPath(base, Color(0xB0FFFFFF), style = Stroke(3f, cap = StrokeCap.Round))
        }
    }
}
