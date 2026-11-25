package com.mizanati.invest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mizanati.invest.core.dm
import com.mizanati.invest.core.dmask
import com.mizanati.invest.core.pd

fun hex(s: String, fallback: Color = Color(0xFF635BFF)): Color = try {
    Color(android.graphics.Color.parseColor(s))
} catch (e: Exception) {
    fallback
}

@Composable
fun cl(x: Double): Color = if (x >= 0) LocalMz.current.a else LocalMz.current.r

@Composable
fun T(
    text: String,
    size: Int = 13,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalMz.current.t,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
) {
    Text(
        text,
        modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        lineHeight = (size * 1.5f).sp,
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val mz = LocalMz.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(mz.c)
            .border(1.dp, mz.b, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

class KV(val k: String, val v: String, val color: Color? = null, val note: String? = null)

@Composable
fun KBox(item: KV, modifier: Modifier = Modifier) {
    val mz = LocalMz.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(mz.c2)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        T(item.k, 11, color = mz.m)
        T(item.v, 14, FontWeight.Bold, item.color ?: mz.t)
        if (item.note != null) T(item.note, 10, color = mz.m)
    }
}

@Composable
fun KGrid(items: List<KV>, columns: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { KBox(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

enum class BtnKind { Primary, Ghost, Danger }

@Composable
fun Btn(
    text: String,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val mz = LocalMz.current
    val shape = RoundedCornerShape(12.dp)
    val bg = when (kind) {
        BtnKind.Primary -> mz.p
        BtnKind.Ghost -> mz.c2
        BtnKind.Danger -> mz.r.copy(alpha = .12f)
    }
    val fg = when (kind) {
        BtnKind.Primary -> Color.White
        BtnKind.Ghost -> mz.t
        BtnKind.Danger -> mz.r
    }
    Box(
        modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * .45f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        T(text, 13, FontWeight.Bold, fg, align = TextAlign.Center)
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    password: Boolean = false,
    ltr: Boolean = false,
) {
    val mz = LocalMz.current
    val shape = RoundedCornerShape(12.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) T(label, 11, FontWeight.Medium, mz.m)
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(
                color = mz.t,
                fontSize = 15.sp,
                textDirection = if (ltr || keyboard != KeyboardType.Text) TextDirection.Ltr else TextDirection.Content,
            ),
            cursorBrush = SolidColor(mz.p),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .clip(shape)
                        .background(mz.c2)
                        .border(1.dp, mz.b, shape)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) T(placeholder, 14, color = mz.m)
                    inner()
                }
            },
        )
    }
}

@Composable
fun NumField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "") {
    Field(label, value, onValue, modifier, KeyboardType.Decimal, placeholder)
}

@Composable
fun DateField(label: String, iso: String, onIso: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(if (iso.isNotEmpty()) dm(iso) else "") }
    Field(
        label,
        text,
        {
            text = dmask(it)
            onIso(pd(text))
        },
        modifier,
        KeyboardType.Number,
        "يوم/شهر/سنة",
    )
}

@Composable
fun <V> Seg(options: List<Pair<V, String>>, selected: V, modifier: Modifier = Modifier, onSelect: (V) -> Unit) {
    val mz = LocalMz.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(mz.c2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (v, label) ->
            val on = v == selected
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) mz.p else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(v) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                T(label, 12, FontWeight.Bold, if (on) Color.White else mz.m, align = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
fun Ring(
    fraction: Double,
    color: Color,
    size: Dp,
    stroke: Dp = 5.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val track = LocalMz.current.b
    val fr = if (fraction.isFinite()) fraction.coerceIn(0.0, 1.0).toFloat() else 0f
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = Offset(w / 2, w / 2)
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, inset, arc, style = Stroke(w))
            if (fr > 0f) drawArc(color, -90f, 360f * fr, false, inset, arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun Donut(parts: List<Pair<Double, Color>>, size: Dp, stroke: Dp = 16.dp, content: @Composable () -> Unit = {}) {
    val track = LocalMz.current.b
    val total = parts.sumOf { if (it.first.isFinite() && it.first > 0) it.first else 0.0 }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = Offset(w / 2, w / 2)
            val arc = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, inset, arc, style = Stroke(w))
            if (total > 0) {
                var start = -90f
                parts.forEach { (v, c) ->
                    if (v.isFinite() && v > 0) {
                        val sweep = (v / total * 360).toFloat()
                        drawArc(c, start, sweep, false, inset, arc, style = Stroke(w))
                        start += sweep
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun Bar(fraction: Double, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp, mark: Double? = null) {
    val mz = LocalMz.current
    val fr = if (fraction.isFinite()) fraction.coerceIn(0.0, 1.0).toFloat() else 0f
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(mz.b, cornerRadius = r)
        val w = size.width * fr
        if (w > 0f) drawRoundRect(color, Offset(size.width - w, 0f), Size(w, size.height), r)
        if (mark != null && mark.isFinite()) {
            val x = size.width - size.width * mark.coerceIn(0.0, 1.0).toFloat()
            drawLine(mz.t, Offset(x, -2f), Offset(x, size.height + 2f), 2.dp.toPx())
        }
    }
}

@Composable
fun Legend(color: Color, label: String, value: String) {
    val mz = LocalMz.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        T(label, 12, modifier = Modifier.weight(1f), maxLines = 1)
        T(value, 12, FontWeight.Bold, mz.m)
    }
}

@Composable
fun Collapsible(title: String, open: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val mz = LocalMz.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(mz.c2)
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T(title, 12, FontWeight.Bold, modifier = Modifier.weight(1f))
            T(if (open) "▴" else "▾", 12, color = mz.m)
        }
        if (open) content()
    }
}

@Composable
fun Note(text: String, color: Color = LocalMz.current.m) {
    T(text, 11, color = color)
}

@Composable
fun MzDialog(
    title: String,
    onClose: () -> Unit,
    full: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val mz = LocalMz.current
        val shape = RoundedCornerShape(22.dp)
        Column(
            Modifier
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .let { if (full) it.fillMaxHeight() else it }
                .clip(shape)
                .background(mz.c)
                .border(1.dp, mz.b, shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(title, 16, FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2)
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(mz.c2)
                        .clickable(role = Role.Button, onClickLabel = "إغلاق", onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { T("✕", 15, FontWeight.Bold, mz.m) }
            }
            Column(
                Modifier
                    .weight(1f, fill = full)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
fun ConfirmDialog(text: String, confirm: String, onClose: () -> Unit, danger: Boolean = true, onConfirm: () -> Unit) {
    MzDialog("تأكيد", onClose) {
        T(text, 14)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(confirm, Modifier.weight(1f), if (danger) BtnKind.Danger else BtnKind.Primary) {
                onConfirm()
                onClose()
            }
            Btn("إلغاء", Modifier.weight(1f), BtnKind.Ghost, onClick = onClose)
        }
    }
}
