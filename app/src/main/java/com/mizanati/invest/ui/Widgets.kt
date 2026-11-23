package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.*

// Shared Compose building blocks styled to match the original app's cards/chips/buttons.

val cardShape = RoundedCornerShape(14.dp)
val chipShape = RoundedCornerShape(10.dp)

@Composable
fun Card2(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalTheme
    Column(
        modifier
            .fillMaxWidth()
            .background(t.card, cardShape)
            .border(1.dp, t.border, cardShape)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val t = LocalTheme
    Text(text, color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 12) {
    Text(text, color = LocalTheme.muted, fontSize = size.sp, modifier = modifier)
}

@Composable
fun Chip(
    text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val t = LocalTheme
    Text(
        text,
        color = if (selected) Color.White else t.text,
        fontSize = 12.sp,
        modifier = modifier
            .background(if (selected) t.primary else t.card2, chipShape)
            .border(1.dp, if (selected) t.primary else t.border, chipShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun Field(value: String, on: (String) -> Unit, label: String, keyboard: Int = 0, modifier: Modifier = Modifier) {
    val t = LocalTheme
    Column(modifier) {
        Muted(label)
        Spacer(Modifier.height(4.dp))
        BasicTextFieldAr(value = value, on = on, modifier = Modifier
            .fillMaxWidth()
            .background(t.card2, chipShape)
            .border(1.dp, t.border, chipShape)
            .padding(10.dp))
    }
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val t = LocalTheme
    Text(
        text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(if (danger) t.red else t.primary, cardShape)
            .clickable { onClick() }
            .padding(vertical = 13.dp),
    )
}

@Composable
fun Row3(a: Pair<String, String>, b: Pair<String, String>? = null, c: Pair<String, String>? = null) {
    val t = LocalTheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOfNotNull(a, b, c).forEach { (k, v) ->
            Column(
                Modifier.weight(1f).background(t.card2, chipShape).padding(10.dp)
            ) {
                Muted(k, size = 11)
                Text(v, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Arabic-numeric-aware basic text field (keeps latin digits like the original). */
@Composable
fun BasicTextFieldAr(value: String, on: (String) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalTheme
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = on,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = t.text, fontSize = 14.sp,
        ),
        cursorBrush = SolidColor(t.primary),
        modifier = modifier,
    )
}

@Composable
fun ProfitText(v: Double, pct: Double) {
    val t = LocalTheme
    val col = if (v >= 0) t.green else t.red
    Text(
        "${Finance.fmt(v, 2)} ج${if (!pct.isNaN()) " (${Finance.fmt(pct, 1)}%)" else ""}",
        color = col, fontSize = 15.sp, fontWeight = FontWeight.Bold,
    )
}

@Composable
fun TabStrip(tabs: List<Pair<String, String>>, current: Int, on: (Int) -> Unit) {
    val t = LocalTheme
    Row(
        Modifier.fillMaxWidth()
            .background(t.card, cardShape)
            .border(1.dp, t.border, cardShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            Text(
                tab.second,
                fontSize = 11.sp,
                fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                color = if (i == current) Color.White else t.muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .background(if (i == current) t.primary else Color.Transparent, chipShape)
                    .clickable { on(i) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}
