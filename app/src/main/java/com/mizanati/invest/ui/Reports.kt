package com.mizanati.invest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Calc
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.HomeSummary
import com.mizanati.invest.core.f
import com.mizanati.invest.core.homeSummary
import com.mizanati.invest.core.insIcon
import com.mizanati.invest.core.nz
import com.mizanati.invest.core.num
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.str
import org.json.JSONObject

private val REPORT_TABS = listOf(
    "score" to "الملخص",
    "rank" to "الأدوات",
    "data" to "البيانات",
    "flow" to "التدفقات",
    "risk" to "المخاطر",
)

@Composable
fun ReportsScreen(app: AppState) {
    var tab by remember { mutableStateOf("score") }
    val version = app.version
    val e = remember(version) { Engine(app.S) }
    val hs = remember(version) { e.homeSummary() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            T("التقارير والأدوات", 18, FontWeight.Bold)
            T("تحليلات استثمارية وأدوات تخطيط مالي", 12, color = LocalMz.current.m)
        }
        Seg(REPORT_TABS, tab) { tab = it }
        when (tab) {
            "rank" -> RankPanel(e, hs)
            "data" -> DataPanel(e, hs)
            "flow" -> FlowPanel(e, hs)
            "risk" -> RiskPanel(e, hs)
            else -> ScorePanel(e, hs)
        }
    }
}

private fun moneyUsd(v: Double): String = "$" + f(v, if (kotlin.math.abs(v) >= 1000) 0 else 2)

private fun profitPct(c: Calc): String =
    if (c.cost > 0) pc((c.value - c.cost) / c.cost) else "—"

@Composable
private fun ScorePanel(e: Engine, hs: HomeSummary) {
    Card {
        Hero(LocalMz.current.p, "القيمة الإجمالية", moneyUsd(hs.tot), "بمقياس الدولار الأمريكي")
        KGrid(
            listOf(
                KV("الربح التراكمي", moneyUsd(hs.profit), color = cl(hs.profit)),
                KV("نسبة الربح", if (hs.ratio.isFinite()) pc(hs.ratio) else "—"),
                KV("XIRR بالدولار", if (hs.xU.a.isFinite()) pc(hs.xU.a) else "—"),
                KV("XIRR بالجنيه", if (hs.xE.a.isFinite()) pc(hs.xE.a) else "—"),
                KV("أثر الصرف (دولار)", if (hs.xU.d.isFinite()) pc(hs.xU.d) else "—", color = cl(hs.xU.d)),
                KV("أثر الصرف (جنيه)", if (hs.xE.d.isFinite()) pc(hs.xE.d) else "—", color = cl(hs.xE.d)),
                KV("متوسط عمر الاستثمار", f(hs.age, 1) + " سنة"),
                KV("توزيع الدولار/الجنيه", f(hs.usdPct * 100, 0) + "% / " + f(hs.egpPct * 100, 0) + "%"),
            ),
        )
        Note("تُحسب XIRR من التدفقات النقدية الفعلية للأدوات داخل المحافظ الظاهرة في الصفحة الرئيسية.")
    }
}

@Composable
private fun RankPanel(e: Engine, hs: HomeSummary) {
    val ranked = hs.rows
        .flatMap { r -> r.L.map { Triple(r.p, it, e.calc(it)) } }
        .sortedByDescending { it.third.value }
    Card {
        T("ترتيب الأدوات حسب القيمة", 15, FontWeight.Bold)
        if (ranked.isEmpty()) T("لا توجد أدوات بعد.", 13)
        ranked.forEach { (p, i, c) ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                T(insIcon(i), 18)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    T(i.str("name"), 13, FontWeight.Bold)
                    T(p.str("name") + " · " + e.sym(p), 11, color = LocalMz.current.m)
                }
                T(e.sym(p) + f(c.value, 0), 13, FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DataPanel(e: Engine, hs: HomeSummary) {
    Card {
        T("بيانات الأدوات", 15, FontWeight.Bold)
        Note("التكلفة والقيمة بالعملة المحلية للأداة، والربح نسبة إلى التكلفة.")
        hs.rows.forEach { r ->
            val items = r.L
            T(r.p.str("name"), 13, FontWeight.Bold, color = hex(r.p.str("col"), LocalMz.current.p))
            if (items.isEmpty()) T("لا توجد أدوات.", 12, color = LocalMz.current.m)
            items.forEach { i ->
                val c = e.calc(i)
                Column {
                    T(insIcon(i) + " " + i.str("name"), 12, FontWeight.Bold)
                    KGrid(
                        listOf(
                            KV("التكلفة", e.sym(r.p) + f(c.cost, 0)),
                            KV("القيمة", e.sym(r.p) + f(c.value, 0)),
                            KV("الربح", profitPct(c), color = cl(c.value - c.cost)),
                            KV("XIRR", if (c.xr.isFinite()) pc(c.xr) else "—"),
                        ),
                        columns = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowPanel(e: Engine, hs: HomeSummary) {
    Card {
        T("التدفقات النقدية", 15, FontWeight.Bold)
        hs.rows.forEach { r ->
            val c = r.A
            KGrid(
                listOf(
                    KV(r.p.str("name") + " · شراء", e.sym(r.p) + f(c.buys, 0)),
                    KV(r.p.str("name") + " · عدد العمليات", f(c.num, 0)),
                    KV(r.p.str("name") + " · قيمة", e.sym(r.p) + f(c.value, 0)),
                    KV(r.p.str("name") + " · XIRR", if (c.xr.isFinite()) pc(c.xr) else "—"),
                ),
            )
        }
        Note("التدفقات تشمل الشراء والاسترداد والتوزيعات المسجلة على الأدوات حتى اليوم.")
    }
}

@Composable
private fun RiskPanel(e: Engine, hs: HomeSummary) {
    val fx = e.S.optJSONObject("fx") ?: JSONObject()
    val total = hs.usdVal + hs.egpVal
    val top = hs.rows
        .flatMap { it.L.map { i -> e.calc(i).value } }
        .maxOrNull() ?: 0.0
    Card {
        T("مؤشرات المخاطر", 15, FontWeight.Bold)
        KGrid(
            listOf(
                KV("نسبة الدولار", pc(hs.usdPct)),
                KV("نسبة الجنيه", pc(hs.egpPct)),
                KV("أكبر أداة من المحفظة", if (total > 0) pc(top / (total * hs.rows.size.coerceAtLeast(1))) else "—"),
                KV("سعر الصرف الدفتري", f(fx.nz("book"), 2) + " ج"),
                KV("سعر الصرف الحالي", f(fx.nz("now"), 2) + " ج"),
                KV("انخفاض الجنيه المسجل", if (fx.num("book") > 0 && fx.num("now") > 0) pc(fx.num("book") / fx.num("now") - 1) else "—"),
            ),
        )
        KGrid(
            listOf(
                KV("صندوق الطوارئ", "$" + f(e.emergencyMarketValue(), 0)),
                KV("تحت خط الأمان؟", if (e.emergencyBelowSafety()) "نعم" else "لا", color = if (e.emergencyBelowSafety()) LocalMz.current.r else LocalMz.current.a),
            ),
        )
        Note("التركّز العالي في أداة واحدة أو عملة واحدة يرفع المخاطر؛ راقب نسب الدولار والجنيه.")
    }
}
