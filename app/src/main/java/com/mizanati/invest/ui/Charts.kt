package com.mizanati.invest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Calc
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.dm
import com.mizanati.invest.core.f
import com.mizanati.invest.core.num
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.str
import com.mizanati.invest.core.tf
import com.mizanati.invest.core.ymd
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

val Green = Color(0xFF10B981)
val Amber = Color(0xFFF59E0B)
val Indigo = Color(0xFF6366F1)

@Composable
fun Gauge(v: Double, t: Double, modifier: Modifier = Modifier) {
    val mz = LocalMz.current
    val ok = v.isFinite()
    val mx = max(max(if (ok) abs(v) else 0.0, t), .01) * 1.25
    fun fr(x: Double) = min(1.0, max(0.0, x / mx))
    val fv = if (ok) fr(max(0.0, v)) else 0.0
    val ft = fr(t)
    val col = if (ok && v >= t) Green else Amber
    Box(
        modifier
            .widthIn(max = 190.dp)
            .fillMaxWidth()
            .aspectRatio(170f / 104f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width / 170f
            val cx = 85 * s
            val cy = 88 * s
            val r = 66 * s
            val sw = 15 * s
            val tl = Offset(cx - r, cy - r)
            val box = Size(2 * r, 2 * r)
            drawArc(mz.b, 180f, 180f, false, tl, box, style = Stroke(sw, cap = StrokeCap.Round))
            if (fv > 0) drawArc(col, 0f, (-180 * fv).toFloat(), false, tl, box, style = Stroke(sw, cap = StrokeCap.Round))
            val a = PI * ft
            fun pt(rr: Float) = Offset(cx + rr * cos(a).toFloat(), cy - rr * sin(a).toFloat())
            drawLine(mz.t, pt(r - sw / 2 - 3 * s), pt(r + sw / 2 + 3 * s), 3 * s, StrokeCap.Round)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(if (ok) pc(v) else "—", 20, FontWeight.ExtraBold, col, maxLines = 1)
            T("الهدف ${tf(t * 100, 1)}%", 10, color = mz.m, maxLines = 1)
        }
    }
}

@Composable
fun StackBar(parts: List<Pair<Double, Color>>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val track = LocalMz.current.b
    val total = parts.sumOf { if (it.first.isFinite() && it.first > 0) it.first else 0.0 }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50)),
    ) {
        drawRect(track)
        if (total > 0) {
            var x = size.width
            parts.forEach { (v, c) ->
                if (v.isFinite() && v > 0) {
                    val w = (v / total).toFloat() * size.width
                    drawRect(c, Offset(x - w, 0f), Size(w, size.height))
                    x -= w
                }
            }
        }
    }
}

@Composable
fun SplitBar(rc: Double, uc: Double) {
    val mz = LocalMz.current
    StackBar(listOf(abs(rc) to Green, abs(uc) to Indigo), height = 9.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(Green))
            T("محقق ${pc(rc)}", 10, color = mz.m)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(Indigo))
            T("متوقع ${pc(uc)}", 10, color = mz.m)
        }
    }
}

@Composable
fun RatioRing(r: Double, real: Double, unr: Double, cost: Double) {
    val mz = LocalMz.current
    val ok = r.isFinite()
    val mx = max((if (ok) abs(r) else 0.0) * 1.35, .12)
    val g = if (ok) min(1.0, abs(r) / mx) else 0.0
    val pos = !ok || r >= 0
    val rc = if (cost > 0) real / cost else 0.0
    val uc = if (cost > 0) unr / cost else 0.0
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Ring(g, if (pos) Green else Color(0xFFEF4444), 104.dp, 11.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                T(pc(r), 16, FontWeight.ExtraBold, if (pos) Color(0xFF0E9F6E) else Color(0xFFE5484D), maxLines = 1)
                T("نسبة الربح", 9, color = mz.m)
            }
        }
        SplitBar(rc, uc)
    }
}

class PB(val n: String, val v: Double, val c: Color? = null)

@Composable
fun ProfitBars(items: List<PB>, cu: String) {
    val mz = LocalMz.current
    val mx = max(1e-9, items.maxOfOrNull { abs(it.v) } ?: 0.0)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { x ->
            val col = x.c ?: if (x.v >= 0) Green else Color(0xFFEF4444)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T(x.n, 11, modifier = Modifier.width(78.dp), maxLines = 1)
                Bar(max(.02, abs(x.v) / mx), col, Modifier.weight(1f), 8.dp)
                T(cu + f(x.v), 11, FontWeight.Bold, if (x.c != null) mz.t else cl(x.v), maxLines = 1)
            }
        }
    }
}

@Composable
fun PathChart(c: Calc, cu: String): Boolean {
    val fl = c.cf.filter { it.v != 0.0 && !it.v.isNaN() }.sortedBy { it.t }
    if (fl.isEmpty()) return false
    val mz = LocalMz.current
    var run = 0.0
    val pts = fl.map {
        run += -it.v
        it.t to run
    }
    val last = run
    val now = System.currentTimeMillis().toDouble()
    val t0 = pts.first().first
    val t1 = max(now, pts.last().first)
    val my = max(max(c.value, pts.maxOf { it.second }), 1.0)
    val purple = Color(0xFF635BFF)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        T(cu + f(c.value), 10, FontWeight.ExtraBold, Green, Modifier.fillMaxWidth(), align = TextAlign.Start)
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(330f / 100f),
        ) {
            val pl = 8f
            val pr = 8f
            fun X(t: Double) = pl + ((t - t0) / (if (t1 - t0 == 0.0) 1.0 else t1 - t0)).toFloat() * (size.width - pl - pr)
            fun Y(y: Double) = 6f + (1 - max(0.0, y) / my).toFloat() * (size.height - 12f)
            val line = Path()
            line.moveTo(X(t0), Y(0.0))
            var prev = 0.0
            pts.forEach { (t, y) ->
                line.lineTo(X(t), Y(prev))
                line.lineTo(X(t), Y(y))
                prev = y
            }
            line.lineTo(X(t1), Y(last))
            val area = Path()
            area.addPath(line)
            area.lineTo(X(t1), Y(0.0))
            area.close()
            drawPath(area, Brush.verticalGradient(listOf(purple.copy(alpha = .38f), purple.copy(alpha = .02f))))
            drawPath(line, purple, style = Stroke(2.4.dp.toPx(), join = StrokeJoin.Round))
            val vy = Y(c.value)
            drawLine(
                Green,
                Offset(pl, vy),
                Offset(size.width - pr, vy),
                1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
            drawCircle(Green, 4.5.dp.toPx(), Offset(X(t1), vy))
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T(dm(ymd(t0)), 10, color = mz.m)
                T("اليوم", 10, color = mz.m)
            }
        }
    }
    return true
}

@Composable
fun ChBox(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val mz = LocalMz.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(mz.c2)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        T(title, 11, FontWeight.Bold, mz.m, Modifier.fillMaxWidth())
        content()
    }
}

@Composable
fun Pill(text: String, color: Color = LocalMz.current.t) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(LocalMz.current.c2)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { T(text, 11, FontWeight.ExtraBold, color, maxLines = 1) }
}

@Composable
fun KG(e: Engine, r: Calc, p: JSONObject, lbl: Boolean = false) {
    val c = e.sym(p)
    val o = e.osym(p)
    val E = p.str("cur") == "EGP"
    val af = if (E) (if (r.buysU != 0.0) r.buys / r.buysU else Double.NaN) else (if (r.buys != 0.0) r.buysU / r.buys else Double.NaN)
    var open by remember { mutableStateOf(false) }
    KGrid(
        listOf(
            KV("القيمة الحالية", c + f(r.value)),
            KV("التكلفة المتبقية", c + f(r.cost)),
            KV("ربح محقق", c + f(r.real), cl(r.real)),
            KV("ربح متوقع (غير محقق)", c + f(r.unr), cl(r.unr)),
            KV("إجمالي الربح", c + f(r.tot), cl(r.tot)),
            KV("XIRR سنوي", pc(r.xr), cl(r.xr)),
            KV("العمر المرجّح", f(r.age) + " يوم"),
        ),
    )
    Collapsible("🌐 تفاصيل الصرف والعملة الأخرى", open, { open = !open }) {
        KGrid(
            listOf(
                KV("متوسط سعر الدولار وقت الشراء", f(af, 2)),
                KV("سعر الدولار الآن", f(e.S.optJSONObject("fx")?.num("now") ?: Double.NaN, 2)),
                KV("التكلفة " + e.oname(p), o + f(r.costU)),
                KV("القيمة " + e.oname(p), o + f(r.valU)),
                KV("إجمالي الربح " + e.oname(p), o + f(r.totU), cl(r.totU)),
                KV(
                    "أثر تغير سعر الصرف",
                    o + f(r.fxe),
                    cl(r.fxe),
                    if (lbl && abs(r.fxe) > 1e-9) (if (r.fxe > 0) "مكسب" else "خسارة") else null,
                ),
                KV("XIRR " + e.oname(p), pc(r.xu), cl(r.xu)),
            ),
        )
    }
}

@Composable
fun Hero(col: Color, small: String, big: String, sub: String, content: @Composable ColumnScope.() -> Unit = {}) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(col, Color(col.red * .62f, col.green * .62f, col.blue * .62f))))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        T(small, 12, color = Color.White.copy(alpha = .86f))
        T(big, 30, FontWeight.ExtraBold, Color.White, maxLines = 1)
        T(sub, 12, color = Color.White.copy(alpha = .86f))
        content()
    }
}

@Composable
fun MiniBull(xr: Double, target: Double, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val sc = max(max(if (xr.isFinite()) abs(xr) else 0.0, target), .01) * 1.25
    val fw = if (xr.isFinite()) min(1.0, max(0.0, xr) / sc) else 0.0
    val ok = xr.isFinite() && xr >= target
    Bar(fw, if (ok) Green else Amber, modifier, height, target / sc)
}

@Suppress("unused")
private val unusedCorner = CornerRadius.Zero
