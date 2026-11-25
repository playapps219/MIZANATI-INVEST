package com.mizanati.invest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.HomeSummary
import com.mizanati.invest.core.dm
import com.mizanati.invest.core.f
import com.mizanati.invest.core.growthParts
import com.mizanati.invest.core.homeSummary
import com.mizanati.invest.core.insIcon
import com.mizanati.invest.core.list
import com.mizanati.invest.core.num
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.pIcon
import com.mizanati.invest.core.profitRatio
import com.mizanati.invest.core.str
import com.mizanati.invest.core.tf
import kotlin.math.abs

private val TITLES = mapOf(
    "xirr" to ("📐 تفصيل العائد XIRR" to "العائد السنوي بالعملات وأثر الصرف"),
    "pf" to ("توزيع المحافظ" to "حصة كل محفظة من ثروتك"),
    "cur" to ("توزيع العملات" to "الدولار والجنيه والمبالغ المطلوبة"),
    "data" to ("بيانات الأدوات" to "سجل وبيانات كل أداة استثمارية"),
    "focus" to ("🎯 بوصلة التركيز والأهداف" to "النسب المستهدفة والمبالغ المطلوبة والفائضة"),
    "growth" to ("↗ نمو العوائد عبر الزمن" to "تحليل المحافظ والأدوات الاستثمارية بالعملتين"),
)

private fun pp(v: Double): String = (if (v >= 0) "\u200E+" else "\u200E−") + tf(abs(v) * 100, 1)

@Composable
fun AnalysisDialog(app: AppState, key: String, onClose: () -> Unit) {
    val version = app.version
    val e = remember(version) { Engine(app.S) }
    val hs = remember(version) { e.homeSummary() }
    val (title, sub) = TITLES[key] ?: return
    MzDialog(title, onClose, full = true) {
        Note(sub)
        when (key) {
            "xirr" -> XirrPanel(hs)
            "pf" -> PfPanel(hs)
            "cur" -> CurPanel(e, hs)
            "data" -> DataPanel(app, e)
            "focus" -> FocusPanel(e)
            "growth" -> GrowthPanel(e)
        }
    }
    val oi = app.openIns
    if (oi != null && app.S.list("ins").any { it.str("id") == oi }) InstrumentDialog(app, oi)
}

@Composable
private fun XirrPanel(hs: HomeSummary) {
    val mz = LocalMz.current
    var egp by remember { mutableStateOf(false) }
    val z = if (egp) hs.xE else hs.xU
    val cn = if (egp) "الجنيه" else "الدولار"
    val total = hs.rows.sumOf { it.usd }.let { if (it == 0.0) 1.0 else it }
    Seg(listOf(false to "الدولار $", true to "الجنيه ج"), egp) { egp = it }
    Card {
        T("📐 العائد السنوي XIRR", 14, FontWeight.ExtraBold)
        T("عائدك السنوي الفعلي محسوباً من تواريخ دفعاتك وقيمة محفظتك اليوم.", 11, color = mz.m)
        KGrid(
            listOf(
                KV("بدون أثر الصرف", pc(z.a), cl(z.a), "أداء الأصول نفسها"),
                KV("مع أثر الصرف", pc(z.f), cl(z.f), "ب$cn الفعلي"),
            ),
        )
        KBox(
            KV("أثر الصرف", if (z.d.isFinite()) pp(z.d) + " نقطة" else "—", if (z.d.isFinite()) cl(z.d) else null,
                if (z.d.isFinite()) (if (z.d >= 0) "مكسب" else "خسارة") else null),
            Modifier.fillMaxWidth(),
        )
        Note(
            if (z.d.isFinite()) {
                "أثر الصرف " + (if (z.d >= 0) "أضاف إلى" else "أنقص من") + " عائدك ب" + cn + " " + pp(z.d) + " نقطة."
            } else {
                "لا يظهر أثر صرف لأن كل أصولك بنفس العملة أو لا توجد بيانات كافية."
            },
        )
    }
    hs.rows.forEach { x ->
        val same = (x.p.str("cur") == "EGP") == egp
        val own = x.A.xr
        val inV = if (same) x.A.xr else x.A.xu
        val dd = if (same) Double.NaN else x.A.xu - x.A.xr
        Card {
            T(x.p.str("name"), 13, FontWeight.ExtraBold)
            T(tf(x.usd / total * 100, 1) + "% من الثروة · " + (if (x.p.str("cur") == "EGP") "جنيه" else "دولار"), 10, color = mz.m)
            KGrid(
                listOf(
                    KV("بعملتها", pc(own), if (own.isFinite()) cl(own) else null),
                    KV("ب$cn", pc(inV), if (inV.isFinite()) cl(inV) else null),
                    KV("أثر الصرف", if (dd.isFinite()) pp(dd) else "—", if (dd.isFinite()) cl(dd) else null),
                ),
            )
        }
    }
    Note("💱 نفس تغيّر سعر الدولار يظهر خسارة بمنظور الدولار (للأصول الجنيهية) ومكسباً بمنظور الجنيه (للأصول الدولارية).")
    Note("ℹ️ «بدون أثر الصرف» يحسب كل دفعاتك بسعر الصرف الحالي فيعزل أداء الأصول. «مع أثر الصرف» يحسب كل دفعة بسعر الصرف يوم تنفيذها.")
}

private val PF_FALLBACK = listOf("#635bff", "#19b8a8", "#ff9f43", "#9b5de5")

@Composable
private fun PfPanel(hs: HomeSummary) {
    if (hs.rows.isEmpty()) {
        Note("لا توجد بيانات.")
        return
    }
    val tot = if (hs.tot == 0.0) 1.0 else hs.tot
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("توزيع المحافظ", 14, FontWeight.ExtraBold)
            T(f(hs.tot) + " USD", 12, color = LocalMz.current.m)
        }
        val colors = hs.rows.mapIndexed { k, x -> hex(x.p.str("col").ifEmpty { PF_FALLBACK[k % 4] }) }
        StackBar(hs.rows.mapIndexed { k, x -> maxOf(0.0, x.usd / tot) to colors[k] }, height = 12.dp)
        hs.rows.forEachIndexed { k, x -> Legend(colors[k], x.p.str("name"), pc(x.usd / tot)) }
    }
}

@Composable
private fun CurPanel(e: Engine, hs: HomeSummary) {
    val mz = LocalMz.current
    val fn = e.fn
    val usdVal = hs.usdVal
    val egpVal = hs.egpVal
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("توزيع العملات", 14, FontWeight.ExtraBold)
            T("من إجمالي المحافظ الظاهرة", 10, color = mz.m)
        }
        StackBar(listOf(hs.usdPct to Indigo, hs.egpPct to Green), height = 12.dp)
        Legend(Indigo, "دولار", pc(hs.usdPct))
        Legend(Green, "جنيه", pc(hs.egpPct))
        val sP = (e.S.optDouble("split", 50.0).let { if (it.isNaN()) 50.0 else it }) / 100
        val t2 = usdVal + egpVal
        if (t2 > 0) {
            val dU = (hs.usdPct - sP) * 100
            val dE = (hs.egpPct - (1 - sP)) * 100
            val nU = t2 * sP - usdVal
            val nE = (t2 * (1 - sP) - egpVal) * fn
            fun tag(d: Double) = if (abs(d) < .05) "مطابق" else if (d > 0) "زيادة" else "نقص"
            fun amt(v: Double, c: String) = if (abs(v) < .5) "لا حاجة" else if (v > 0) c + f(v) + " إضافة" else c + f(-v) + " تخفيض"
            Note("النسب المحفوظة في الإعدادات: دولار ${tf(sP * 100, 0)}% · جنيه ${tf((1 - sP) * 100, 0)}%")
            KGrid(
                listOf(
                    KV("انحراف الدولار", (if (dU >= 0) "+" else "") + tf(dU, 1) + "%", if (abs(dU) > 5) mz.r else mz.a, tag(dU)),
                    KV("انحراف الجنيه", (if (dE >= 0) "+" else "") + tf(dE, 1) + "%", if (abs(dE) > 5) mz.r else mz.a, tag(dE)),
                    KV("المطلوب للدولار", amt(nU, "$"), if (abs(nU) < .5 || nU > 0) mz.a else mz.r),
                    KV("المطلوب للجنيه", amt(nE, "ج"), if (abs(nE) < .5 || nE > 0) mz.a else mz.r),
                ),
            )
            val add = if (dU < -.5 && sP < 1) {
                "بدون بيع: استثمر ≈ $" + f((sP * t2 - usdVal) / (1 - sP)) + " في الدولار لتصل للنسبة المحفوظة."
            } else if (dU > .5 && sP > 0) {
                "بدون بيع: استثمر ≈ ج" + f(((1 - sP) * t2 - egpVal) / sP * fn) + " في الجنيه لتصل للنسبة المحفوظة."
            } else {
                "التوزيع مطابق أو قريب جداً من النسب المحفوظة."
            }
            Note(add)
        }
    }
}

@Composable
private fun DataPanel(app: AppState, e: Engine) {
    val mz = LocalMz.current
    var mode by remember { mutableStateOf("ins") }
    var pf by remember { mutableStateOf("all") }
    var kind by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("date") }
    var desc by remember { mutableStateOf(true) }
    var lim by remember { mutableStateOf(8) }
    val fn = e.fn
    val visible = e.pfs.filter { it.opt("showHome") != false }
    class Row2(val i: org.json.JSONObject, val p: org.json.JSONObject, val c: com.mizanati.invest.core.Calc, val usd: Double, val profit: Double, val first: String)
    val all = visible.flatMap { p ->
        val eg = p.str("cur") == "EGP"
        e.insOf(p).map { i ->
            val c = e.fin(e.calc(i), eg)
            Row2(i, p, c, if (eg) c.value / fn else c.value, if (eg) c.totU else c.tot, c.first?.takeIf { it.isFinite() }?.let { com.mizanati.invest.core.ymd(it) } ?: "")
        }
    }.filter { (pf == "all" || it.p.str("id") == pf) && (kind == "all" || it.i.str("kind") == kind) }

    Seg(listOf("ins" to "📋 بيانات الأدوات", "tx" to "🧾 سجل العمليات"), mode) { mode = it; lim = if (it == "tx") 20 else 8 }
    Card {
        T("🔎 الفلاتر والترتيب", 12, FontWeight.ExtraBold)
        Seg(listOf("all" to "كل المحافظ") + visible.map { it.str("id") to it.str("name") }, pf) { pf = it }
        Seg(listOf("all" to "الكل", "fund" to "وثائق", "gold" to "ذهب", "cert" to "شهادات", "prop" to "عقار"), kind) { kind = it }
        val sorts = if (mode == "tx") listOf("date" to "التاريخ", "value" to "المبلغ", "name" to "الاسم")
        else listOf("date" to "أول عملية", "xirr" to "XIRR", "profit" to "الربح", "value" to "القيمة", "name" to "الاسم")
        Seg(sorts, if (sorts.any { it.first == sort }) sort else "date") { sort = it }
        Btn(if (desc) "↓ تنازلي" else "↑ تصاعدي", Modifier.fillMaxWidth(), BtnKind.Ghost) { desc = !desc }
    }
    val sg = if (desc) -1 else 1
    if (mode == "ins") {
        val rows = all.sortedWith { a, b ->
            sg * when (sort) {
                "xirr" -> (if (a.c.xr.isFinite()) a.c.xr else -1e9).compareTo(if (b.c.xr.isFinite()) b.c.xr else -1e9)
                "profit" -> a.profit.compareTo(b.profit)
                "value" -> a.usd.compareTo(b.usd)
                "name" -> a.i.str("name").compareTo(b.i.str("name"))
                else -> a.first.compareTo(b.first)
            }
        }
        val sv = rows.sumOf { it.usd }
        val sp = rows.sumOf { it.profit }
        T("${rows.size} أداة · القيمة $" + f(sv) + " · الربح $" + f(sp), 11, FontWeight.Bold, cl(sp))
        rows.take(lim).forEach { o ->
            val cu = e.sym(o.p)
            Card(onClick = { app.openIns = o.i.str("id") }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(insIcon(o.i), 18)
                    Column(Modifier.weight(1f)) {
                        T(o.i.str("name"), 13, FontWeight.ExtraBold, maxLines = 1)
                        T(pIcon(o.p) + " " + o.p.str("name") + (if (o.first.isNotEmpty()) " · " + dm(o.first) else ""), 10, color = mz.m, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        T(cu + f(o.c.value), 13, FontWeight.ExtraBold)
                        T(pc(profitRatio(o.c)) + " · XIRR " + pc(o.c.xr), 10, color = cl(profitRatio(o.c)))
                    }
                }
            }
        }
        if (rows.size > lim) Btn("عرض المزيد (${rows.size - lim} متبقٍ)", Modifier.fillMaxWidth(), BtnKind.Ghost) { lim += 10 }
    } else {
        class TxRow(val t: org.json.JSONObject, val o: Row2, val usd: Double)
        val txs = all.flatMap { o ->
            val eg = o.p.str("cur") == "EGP"
            o.i.list("tx").map { t -> TxRow(t, o, if (eg) t.num("amt") / (t.num("fx").let { if (it > 0) it else fn }) else t.num("amt")) }
        }.sortedWith { a, b ->
            sg * when (sort) {
                "value" -> a.usd.compareTo(b.usd)
                "name" -> a.o.i.str("name").compareTo(b.o.i.str("name"))
                else -> a.t.str("date").compareTo(b.t.str("date"))
            }
        }
        T("${txs.size} عملية", 11, FontWeight.Bold)
        txs.take(lim).forEach { x ->
            val cu = e.sym(x.o.p)
            Card(onClick = { app.openIns = x.o.i.str("id") }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        T((if (x.t.str("type") == "buy") "شراء" else "استرداد") + " · " + x.o.i.str("name"), 12, FontWeight.Bold, maxLines = 1)
                        T(dm(x.t.str("date")) + " · " + x.o.p.str("name"), 10, color = mz.m, maxLines = 1)
                    }
                    T(cu + f(x.t.num("amt"), 2), 13, FontWeight.ExtraBold)
                }
            }
        }
        if (txs.size > lim) Btn("عرض المزيد (${txs.size - lim} متبقٍ)", Modifier.fillMaxWidth(), BtnKind.Ghost) { lim += 10 }
    }
}

@Composable
private fun FocusPanel(e: Engine) {
    val mz = LocalMz.current
    var filter by remember { mutableStateOf("all") }
    val fx = e.fn
    val base = e.totalPortfolioUSD
    val used = e.pctAllocated()
    class X(val i: org.json.JSONObject, val p: org.json.JSONObject, val cur: String, val value: Double, val target: Double, val pct: Boolean, val alloc: Double) {
        val gap get() = target - value
    }
    val all = e.ins.mapNotNull { i ->
        val p = e.pfs.firstOrNull { it.str("id") == i.str("pf") } ?: return@mapNotNull null
        val eg = p.str("cur") == "EGP"
        X(i, p, e.sym(p), e.fin(e.calc(i), eg).value, e.insTgt(i), i.str("tgtMode") == "pct", i.num("tgtPct").let { if (it.isNaN()) 0.0 else it })
    }
    val targeted = all.filter { it.target > 0 || it.pct }
    val needs = targeted.filter { it.gap > .005 }
    val overs = targeted.filter { it.gap < -.005 }
    val shown = when (filter) {
        "need" -> needs.sortedByDescending { it.gap }
        "over" -> overs.sortedBy { it.gap }
        else -> targeted.sortedByDescending { it.gap }
    }
    fun usd(x: X, v: Double) = v / (if (x.p.str("cur") == "EGP") fx else 1.0)
    Note("لوحة لمتابعة توزيع النسب بين الأدوات، ومعرفة المبالغ التي تحتاج إلى استثمار أو التي تجاوزت أهدافها.")
    KGrid(
        listOf(
            KV("إجمالي النسب المستخدمة", tf(used, 2) + "%"),
            KV("المتاح من 100%", tf(maxOf(0.0, 100 - used), 2) + "%"),
            KV("الأدوات المستهدفة", targeted.size.toString()),
        ),
    )
    Seg(
        listOf("all" to "الكل (${targeted.size})", "need" to "تحتاج تمويلاً (${needs.size})", "over" to "فائض (${overs.size})"),
        filter,
    ) { filter = it }
    if (shown.isEmpty()) Note("لا توجد أدوات مطابقة لهذا الفلتر. حدّد أهداف أدواتك من تفاصيل الأداة.")
    shown.forEach { x ->
        val progress = if (x.target > 0) x.value / x.target else 0.0
        val state = if (x.gap > .005) "مطلوب للاستثمار" else if (x.gap < -.005) "فائض عن الهدف" else "الهدف مكتمل"
        val col = if (x.gap > .005) mz.a else if (x.gap < -.005) mz.r else mz.m
        val targetPct = if (base > 0) usd(x, x.target) / base * 100 else 0.0
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                T(insIcon(x.i), 18)
                Column(Modifier.weight(1f)) {
                    T(x.i.str("name"), 13, FontWeight.ExtraBold, maxLines = 1)
                    T(x.p.str("name") + " · القيمة " + x.cur + f(x.value), 10, color = mz.m, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    T(x.cur + f(x.target), 13, FontWeight.ExtraBold)
                    T("الهدف", 10, color = mz.m)
                }
            }
            Bar(progress.coerceIn(0.0, 1.0), col, Modifier.fillMaxWidth(), 6.dp)
            T(state + ": " + x.cur + f(abs(x.gap)), 11, FontWeight.Bold, col)
            T(
                tf(progress * 100, 1) + "% إنجاز · " +
                    if (x.pct) "تخصيص مستهدف ${tf(x.alloc, 2)}% من إجمالي المحافظ" else "الهدف يعادل ${tf(targetPct, 2)}% من إجمالي المحافظ",
                10, color = mz.m,
            )
        }
    }
    val tgtUsd = targeted.sumOf { usd(it, it.target) }
    val needUsd = needs.sumOf { usd(it, it.gap) }
    val overUsd = overs.sumOf { usd(it, abs(it.gap)) }
    Note("مجموع الأهداف المعروضة ≈ $" + f(tgtUsd) + " · المطلوب $" + f(needUsd) + " · الفائض $" + f(overUsd))
}

@Composable
private fun GrowthPanel(e: Engine) {
    val mz = LocalMz.current
    var out by remember { mutableStateOf("USD") }
    var mode by remember { mutableStateOf("historical") }
    val fn = e.fn
    val sym = if (out == "USD") "$" else "ج"
    Seg(listOf("USD" to "بالدولار $", "EGP" to "بالجنيه ج"), out) { out = it }
    Seg(listOf("historical" to "بسعر العملية", "current" to "بسعر اليوم"), mode) { mode = it }
    val visible = e.pfs.filter { it.opt("showHome") != false }
    var totBasis = 0.0
    var totProfit = 0.0
    class G(val p: org.json.JSONObject, val basis: Double, val profit: Double, val n: Int)
    val rows = visible.map { p ->
        val eg = p.str("cur") == "EGP"
        val L = e.insOf(p)
        val avg = e.growthAvgFx(L, fn)
        var b = 0.0
        var pr = 0.0
        var n = 0
        for (i in L) {
            val g = e.growthParts(i, p, e.fin(e.calc(i), eg), mode, out, avg, fn)
            b += g.basis
            pr += g.events.fold(0.0) { a, ev -> a + ev.v } + g.unrealized
            n++
        }
        totBasis += b
        totProfit += pr
        G(p, b, pr, n)
    }
    val ratio = if (totBasis > 0) totProfit / totBasis else Double.NaN
    Hero(Color(0xFF6366F1), "إجمالي نمو العوائد", (if (totProfit < 0) "\u200E−" else "") + sym + f(abs(totProfit)), "على أساس رأس مال " + sym + f(totBasis)) {
        KGrid(listOf(KV("نسبة الربح", pc(ratio), cl(if (ratio.isFinite()) ratio else 0.0)), KV("عدد المحافظ", rows.size.toString())))
    }
    rows.forEach { g ->
        val r = if (g.basis > 0) g.profit / g.basis else Double.NaN
        Card {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T(pIcon(g.p) + " " + g.p.str("name"), 13, FontWeight.ExtraBold)
                T(g.n.toString() + " أداة", 10, color = mz.m)
            }
            KGrid(
                listOf(
                    KV("الربح (محقق + متوقع)", (if (g.profit < 0) "\u200E−" else "") + sym + f(abs(g.profit)), cl(g.profit)),
                    KV("نسبة الربح", pc(r), if (r.isFinite()) cl(r) else null),
                    KV("رأس المال", sym + f(g.basis)),
                ),
            )
        }
    }
    if (rows.isEmpty()) Note("لا توجد بيانات.")
    Note("يُحسب الربح كمجموع المحقق من العمليات والمتوقع على القيمة الحالية، بتحويل كل عملية بسعر صرفها المسجّل أو بسعر اليوم.")
}
