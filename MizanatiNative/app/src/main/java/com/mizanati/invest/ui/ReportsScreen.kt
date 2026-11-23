package com.mizanati.invest.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.calc.Finance.fmt
import com.mizanati.invest.calc.Finance.pc
import com.mizanati.invest.data.AppState
import com.mizanati.invest.data.Goal
import com.mizanati.invest.data.PayPlan
import com.mizanati.invest.data.uid
import com.mizanati.invest.data.today
import com.mizanati.invest.calc.Finance.addMonths
import com.mizanati.invest.calc.Finance.monthsBetween
import com.mizanati.invest.calc.Finance.parseDate

// Reports & tools (التقارير والأدوات) — 8 tabs like the original:
// الملخص | الأدوات | البيانات | التدفقات | المخاطر | خطة الدفعات | الحرية المالية | أهداف مستقبلية

private val reportTabs = listOf(
    "score" to "الملخص", "rank" to "الأدوات", "data" to "البيانات",
    "flow" to "التدفقات", "risk" to "المخاطر", "plan" to "خطة الدفعات",
    "freedom" to "الحرية المالية", "goals" to "أهداف مستقبلية",
)

@Composable
fun ReportsScreen(s: AppState, onMutate: ((AppState) -> Unit) -> Unit, onExportExcel: () -> Unit, onExportTemplate: () -> Unit) {
    val t = LocalTheme
    var tab by remember { mutableIntStateOf(0) }
    var sortBy by remember { mutableStateOf("xirr") }

    Column(Modifier.fillMaxSize().background(t.bg).padding(16.dp)) {
        TabStrip(reportTabs, tab) { tab = it }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (reportTabs[tab].first) {
                "score" -> ScoreTab(s)
                "rank" -> RankTab(s, sortBy) { sortBy = it }
                "data" -> DataTab(s, onExportExcel, onExportTemplate)
                "flow" -> FlowTab(s)
                "risk" -> RiskTab(s)
                "plan" -> PlanTab(s, onMutate)
                "freedom" -> FreedomTab(s, onMutate)
                "goals" -> GoalsTab(s, onMutate)
            }
        }
    }
}

// ---------- الملخص: نقاط + بلوكات + تنبيهات ----------

@Composable
private fun ScoreTab(s: AppState) {
    val t = LocalTheme
    val tot = Finance.totals(s)
    val (score, parts) = qualityScore(s, tot)
    val col = when {
        score >= 80 -> Color(0xFF34D399)
        score >= 60 -> Color(0xFFA3E635)
        score >= 40 -> Color(0xFFFBBF24)
        else -> Color(0xFFFB7185)
    }
    val grade = when {
        score >= 80 -> "ممتاز"
        score >= 60 -> "جيد"
        score >= 40 -> "يحتاج متابعة"
        else -> "يحتاج اهتمامًا"
    }
    Card2 {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ring(score / 100f, "#${Integer.toHexString((col.hashCode()) and 0xFFFFFF)}", 84f)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("$score / 100", color = col, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(grade, color = col, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Muted("تقييم مركب من الأداء والتنويع والعملات والانتظام", size = 10)
            }
        }
        Spacer(Modifier.height(10.dp))
        parts.forEach { (name, v, _, sub) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Muted(sub, size = 10)
                    Bar((v?.toFloat() ?: 0f), "#635bff")
                }
                Text(if (v == null) "—" else pc(v * 100, 0), color = t.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // insights (تنبيهات ذكية)
    Card2 {
        SectionTitle("تنبيهات ونتائج")
        Spacer(Modifier.height(8.dp))
        insights(s, tot).forEach { (kind, icon, msg) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(icon, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text(msg, color = when (kind) {
                    "ok" -> t.green; "warn" -> t.orange
                    else -> t.muted
                }, fontSize = 12.sp)
            }
        }
    }

    // hero totals
    Card2 {
        Row3(
            "إجمالي القيمة الحالية" to "${fmt(tot.value, 0)} ج",
            "إجمالي الربح الحالي" to "${fmt(tot.profit, 0)} ج",
            "نسبة الربح" to pc(tot.pct),
        )
        Spacer(Modifier.height(6.dp))
        Row3(
            "بالدولار" to "${fmt(tot.valueUsd, 0)} $",
            "XIRR" to pc(tot.xirr),
            "نسبة الدولار" to pc(tot.usdPct * 100, 1),
        )
    }
}

private fun qualityScore(s: AppState, tot: Finance.Totals): Pair<Int, List<Quadruple>> {
    val live = s.ins.filter { Finance.insStats(it, s.fx.now).value > 0 }
    val totV = live.sumOf { Finance.insStats(it, s.fx.now).value }
    val hhi = live.sumOf { (Finance.insStats(it, s.fx.now).value / totV.coerceAtLeast(1.0)).pow(2.0) }
    var pw = 0.0; var ps = 0.0
    s.pfs.filter { !it.isEmergency }.forEach { p ->
        val v = s.ins.filter { it.pf == p.id }.sumOf { Finance.insStats(it, s.fx.now).value }
        if (v > 0 && p.tRet > 0) {
            val st = Finance.pfStats(p, s.ins, s.fx.now)
            if (!st.xr.isNaN()) { ps += (st.xr * 100 / p.tRet).coerceIn(0.0, 1.0) * v; pw += v }
        }
    }
    val nowM = Finance.monthsBetween("2000-01-01", today())
    val invMonths = (5 downTo 0).count { back ->
        val cal = java.util.Calendar.getInstance(); cal.add(java.util.Calendar.MONTH, -back)
        val key = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(cal.time)
        s.ins.any { i -> i.tx.any { it.d.startsWith(key) } }
    }
    val parts = listOf(
        Quadruple("الأداء مقابل الهدف", if (pw > 0) ps / pw else null, .35, "XIRR مقارنة بالعائد المستهدف لكل محفظة"),
        Quadruple("التنويع", if (live.size > 1) ((1 - hhi) / .8).coerceIn(0.0, 1.0) else 0.0, .25, "توزيع القيمة بين الأدوات (تركّز أقل = أفضل)"),
        Quadruple("توازن العملات", (1 - Math.abs(tot.usdPct * 100 - s.split) / 30.0).coerceIn(0.0, 1.0), .2, "القرب من نسبة الدولار المستهدفة"),
        Quadruple("انتظام الاستثمار", invMonths / 6.0, .2, "أشهر فيها دفعات من آخر 6 أشهر"),
    )
    val w = parts.sumOf { (it.second ?: 0.0) * it.third }
    val sc = if (w > 0) (100 * parts.sumOf { (it.second ?: 0.0) * (it.second ?: 0.0) * it.third } / w).toInt() else 0
    return sc to parts
}

data class Quadruple(val first: String, val second: Double?, val third: Double, val fourth: String)

private fun insights(s: AppState, tot: Finance.Totals): List<Triple<String, String, String>> {
    val out = mutableListOf<Triple<String, String, String>>()
    val ins = s.ins.map { it to Finance.insStats(it, s.fx.now) }.filter { (i, st) -> st.cost > 0 && !st.xr.isNaN() }
    if (ins.isNotEmpty()) {
        val best = ins.maxByOrNull { it.second.xr }!!
        out.add(Triple("ok", "🏆", "أفضل أداة: ${best.first.name} بعائد سنوي ${pc(best.second.xr)}"))
        val worst = ins.minByOrNull { it.second.xr }!!
        if (ins.size > 1 && worst.second.xr < 0) out.add(Triple("warn", "📉", "أضعف أداة: ${worst.first.name} بعائد ${pc(worst.second.xr)}"))
    }
    if (tot.value > 0) {
        val top = s.ins.map { it to Finance.insStats(it, s.fx.now) }.maxByOrNull { it.second.value }
        if (top != null && top.second.value / tot.value > .3)
            out.add(Triple("warn", "⚖️", "تركّز مرتفع: ${top.first.name} يمثل ${pc(top.second.value / tot.value * 100)} من ثروتك"))
    }
    val dev = (tot.usdPct * 100 - s.split)
    out.add(
        if (Math.abs(dev) > 5) Triple("warn", "💱", "نسبة الدولار ${pc(tot.usdPct * 100)} مقابل مستهدف ${s.split}% (انحراف ${fmt(dev, 1)}%)")
        else Triple("ok", "💱", "توزيع العملات قريب من المستهدف")
    )
    if (s.fx.book > 0) {
        val avg = s.fx.book
        val sN = s.fx.now / avg - 1
        if (Math.abs(sN) > .001) out.add(Triple("info", "🌐", "سعر الدولار الآن ${if (sN > 0) "أعلى" else "أقل"} من متوسط سعر محافظك (${fmt(avg, 2)} ج) بـ ${fmt(Math.abs(sN) * 100, 1)}% — شاهد أثر ذلك في المخاطر"))
    }
    return out
}

// ---------- الأدوات: ترتيب ----------

@Composable
private fun RankTab(s: AppState, sortBy: String, onSort: (String) -> Unit) {
    val t = LocalTheme
    Card2 {
        SectionTitle("ترتيب الأدوات")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("xirr" to "العائد السنوي", "profit" to "الربح", "value" to "القيمة", "ratio" to "نسبة الربح").forEach { (k, l) ->
                Chip(l, sortBy == k) { onSort(k) }
            }
        }
        Spacer(Modifier.height(10.dp))
        val rows = s.ins.map { i ->
            val p = s.pfs.firstOrNull { it.id == i.pf }
            val st = Finance.insStats(i, s.fx.now)
            Triple(i, st, p)
        }.filter { it.third != null && it.second.cost > 0 }
        val sorted = when (sortBy) {
            "profit" -> rows.sortedByDescending { it.second.total }
            "value" -> rows.sortedByDescending { it.second.value }
            "ratio" -> rows.sortedByDescending { if (it.second.cost > 0) it.second.total / it.second.cost else Double.NaN }
            else -> rows.sortedByDescending { it.second.xr }
        }
        val medals = listOf("🥇", "🥈", "🥉")
        sorted.forEachIndexed { k, (i, st, p) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (k < 3) medals[k] else "${k + 1}", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = t.muted, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(i.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Muted("${p?.name ?: ""} · ${p?.secs?.firstOrNull { it.id == i.sec }?.name ?: i.kind}", size = 10)
                    Bar(
                        ((when (sortBy) {
                            "profit" -> st.total; "value" -> st.value
                            "ratio" -> if (st.cost > 0) st.total / st.cost else 0.0
                            else -> st.xr
                        }).let { if (it.isNaN()) 0.0 else Math.abs(it) }).toFloat().let { v ->
                            val mx = sorted.maxOfOrNull { (i2, st2, _) ->
                                Math.abs(when (sortBy) {
                                    "profit" -> st2.total; "value" -> st2.value
                                    "ratio" -> if (st2.cost > 0) st2.total / st2.cost else 0.0
                                    else -> st2.xr
                                }.let { x -> if (x.isNaN()) 0.0 else x })
                            } ?: 1.0
                            (v / mx).toFloat()
                        },
                        p?.col ?: "#2f6df6",
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        when (sortBy) {
                            "profit" -> "${fmt(st.total, 0)} ${Finance.curSym(p?.cur ?: "EGP")}"
                            "value" -> "${fmt(st.value, 0)} ${Finance.curSym(p?.cur ?: "EGP")}"
                            "ratio" -> pc(if (st.cost > 0) st.total / st.cost * 100 else Double.NaN)
                            else -> pc(st.xr)
                        },
                        color = if ((if (sortBy == "profit") st.total else st.xr) >= 0) t.green else t.red,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                    Muted(st.ageDays.toString() + " يوم", size = 10)
                }
            }
        }
    }
}

// ---------- البيانات: تصدير/استيراد ----------

@Composable
private fun DataTab(s: AppState, onExportExcel: () -> Unit, onExportTemplate: () -> Unit) {
    val t = LocalTheme
    Card2 {
        SectionTitle("تصدير البيانات")
        Spacer(Modifier.height(8.dp))
        Muted("احفظ بياناتك في ملف Excel أو استخدم قالبًا جاهزًا للاستيراد الجماعي.", size = 11)
        Spacer(Modifier.height(10.dp))
        BigButton("تصدير Excel (xlsx)") { onExportExcel() }
        Spacer(Modifier.height(8.dp))
        BigButton("تنزيل قالب Excel فارغ") { onExportTemplate() }
        Spacer(Modifier.height(8.dp))
        Muted("النسخ الاحتياطي الكامل والاستيراد في تبويب الإعدادات.", size = 11)
    }
    Card2 {
        SectionTitle("ملخص البيانات")
        Spacer(Modifier.height(6.dp))
        Row3(
            "محافظ" to "${s.pfs.size}",
            "أدوات" to "${s.ins.size}",
            "حركات" to "${s.ins.sumOf { it.tx.size }}",
        )
        Spacer(Modifier.height(6.dp))
        Row3(
            "أهداف" to "${s.goals.size}",
            "خطط" to "${s.plans.size}",
            "حركات طوارئ" to "${s.emergencyFund.transactions.size}",
        )
    }
}

// ---------- التدفقات ----------

@Composable
private fun FlowTab(s: AppState) {
    val t = LocalTheme
    var deposits = 0.0; var withdrawals = 0.0
    s.ins.forEach { i ->
        i.tx.forEach { t -> if (t.amt > 0) deposits += t.amt else withdrawals += -t.amt }
    }
    val net = deposits - withdrawals
    val months = 12
    val cal = java.util.Calendar.getInstance()
    val monthly = (months - 1 downTo 0).map { back ->
        val c = cal.clone() as java.util.Calendar
        c.add(java.util.Calendar.MONTH, -back)
        val key = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(c.time)
        val inflow = s.ins.flatMap { it.tx }.filter { it.d.startsWith(key) && it.amt > 0 }.sumOf { it.amt }
        val outflow = s.ins.flatMap { it.tx }.filter { it.d.startsWith(key) && it.amt < 0 }.sumOf { -it.amt }
        key to (inflow to outflow)
    }
    val maxV = monthly.maxOf { maxOf(it.second.first, it.second.second) }.coerceAtLeast(1.0)

    Card2 {
        SectionTitle("التدفقات")
        Spacer(Modifier.height(6.dp))
        Row3(
            "إجمالي الإيداع" to "${fmt(deposits, 0)}",
            "إجمالي السحب" to "${fmt(withdrawals, 0)}",
            "صافي التدفق" to "${fmt(net, 0)}",
        )
        Spacer(Modifier.height(10.dp))
        SectionTitle("التدفقات الشهرية (12 شهرًا)")
        monthly.forEach { (key, io) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Muted(key.substring(5), size = 10, modifier = Modifier.width(30.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth().height(8.dp)) {
                        Box(Modifier.fillMaxHeight().weight((io.first / maxV).toFloat()).background(t.green, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(2.dp))
                        Box(Modifier.fillMaxHeight().weight((io.second / maxV).toFloat()).background(t.red, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                    }
                }
                Muted("+" + fmt(io.first, 0) + " / -" + fmt(io.second, 0), size = 10)
            }
        }
    }

    // انتظام
    val nowM = monthsBetween("2000-01-01", today())
    Card2 {
        SectionTitle("انتظام الاستثمار (آخر 12 شهرًا)")
        val active = monthly.count { it.second.first > 0 }
        Text(
            if (active >= 10) "انتظام ممتاز — استثمرت في $active من 12 شهرًا"
            else if (active >= 6) "انتظام جيد — استثمرت في $active من 12 شهرًا"
            else "انتظام ضعيف — استثمرت في $active من 12 شهرًا",
            color = if (active >= 10) t.green else if (active >= 6) t.orange else t.red,
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
    }
}

// ---------- المخاطر ----------

@Composable
private fun RiskTab(s: AppState) {
    val t = LocalTheme
    val tot = Finance.totals(s)
    val fx = s.fx.now
    val live = s.ins.map { it to Finance.insStats(it, s.fx.now) }.filter { it.second.value > 0 }
    val totV = live.sumOf { it.second.value }

    Card2 {
        SectionTitle("المخاطر الرئيسية")
        Spacer(Modifier.height(8.dp))
        // تركّز
        val top = live.maxByOrNull { it.second.value }
        if (top != null && totV > 0) {
            val pct = top.second.value / totV * 100
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("⚖️", fontSize = 15.sp); Spacer(Modifier.width(6.dp))
                Text("تركّز الأدوات: ${top.first.name} يمثل ${pc(pct)} من الثروة — ${if (pct > 30) "مرتفع ⚠️" else if (pct > 25) "متوسط" else "منخفض ✓"}",
                    color = if (pct > 30) t.red else if (pct > 25) t.orange else t.green, fontSize = 12.sp)
            }
        }
        // سعر الصرف
        val usdPct = tot.usdPct * 100
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("💱", fontSize = 15.sp); Spacer(Modifier.width(6.dp))
            Text("نسبة الدولار: ${pc(usdPct)} مقابل مستهدف ${s.split}% — ${if (Math.abs(usdPct - s.split) > 5) "انحراف مرتفع ⚠️" else "مطابق ✓"}",
                color = if (Math.abs(usdPct - s.split) > 5) t.orange else t.green, fontSize = 12.sp)
        }
        // لو تغيّر الدولار
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("🌐", fontSize = 15.sp); Spacer(Modifier.width(6.dp))
            Text("لو تغيّر سعر الدولار ±10%: أثر ≈ ${fmt(tot.usdVal * fx * .1, 0)} ج على القيمة الكلية",
                color = t.muted, fontSize = 12.sp)
        }
        // سيولة سريعة (نقد وشهادات)
        val liquid = live.filter { (i, _) -> i.kind == "cash" || i.kind == "cert" }.sumOf { it.second.value }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("💧", fontSize = 15.sp); Spacer(Modifier.width(6.dp))
            Text("سيولة سريعة (نقد وشهادات): ${fmt(liquid, 0)} ${pc(if (totV > 0) liquid / totV * 100 else Double.NaN)}",
                color = t.muted, fontSize = 12.sp)
        }
    }

    Card2 {
        SectionTitle("ثروتك بعملتين")
        Spacer(Modifier.height(6.dp))
        Row3(
            "بالجنيه" to "${fmt(tot.egpVal, 0)} ج",
            "بالدولار" to "${fmt(tot.usdVal, 0)} $",
            "متوسط السعر" to fmt(s.fx.book, 2),
        )
    }
}

// ---------- خطة الدفعات ----------

@Composable
private fun PlanTab(s: AppState, onMutate: ((AppState) -> Unit) -> Unit) {
    val t = LocalTheme
    Card2 {
        SectionTitle("خطة الدفعات (${s.plans.size})")
        Spacer(Modifier.height(6.dp))
        if (s.plans.isEmpty()) Muted("لم تضع خطة دفعات بعد — حدّد أهداف أدواتك وابدأ خطة شهرية", size = 11)
        s.plans.forEach { c ->
            val p = s.pfs.firstOrNull { it.id == c.pf }
            val total = c.pmt * c.n
            val done = maxOf(0, monthsBetween(c.start, today()))
            val remaining = (c.n - done).coerceAtLeast(0)
            Column(Modifier.fillMaxWidth().background(t.card2, chipShape).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Muted("${p?.name ?: c.pf} · ${fmt(c.pmt, 0)} × ${c.n} شهر بدءًا من ${c.start}", size = 10)
                    }
                    Text("حذف", color = t.red, fontSize = 11.sp, modifier = Modifier.clickable {
                        onMutate { st -> st.plans.removeAll { it.id == c.id } }
                    })
                }
                Spacer(Modifier.height(4.dp))
                Bar((done.toDouble() / c.n.coerceAtLeast(1)).toFloat(), "#635bff")
                Muted("مدفوع $done من $c.n · متبقٍ ${fmt(remaining * c.pmt, 0)} ${Finance.curSym(c.cur)}", size = 10)
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    PlanEditor(s, onMutate)
}

@Composable
private fun PlanEditor(s: AppState, onMutate: ((AppState) -> Unit) -> Unit) {
    val t = LocalTheme
    var name by remember { mutableStateOf("") }
    var pmt by remember { mutableStateOf("") }
    var n by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today()) }
    var pfId by remember { mutableStateOf("") }

    Card2 {
        SectionTitle("إضافة خطة دفعات")
        Spacer(Modifier.height(6.dp))
        Muted("اختر المحفظة", size = 11)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.pfs.filter { !it.isEmergency }.take(3).forEach { p ->
                Chip(p.name, pfId == p.id) { pfId = p.id }
            }
        }
        Spacer(Modifier.height(6.dp))
        Field(name, { name = it }, "اسم الخطة")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(pmt, { pmt = it }, "القسط الشهري", Modifier.weight(1f))
            Field(n, { n = it }, "عدد الأشهر", Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Field(start, { start = it }, "تاريخ البداية (yyyy-MM-dd)")
        Spacer(Modifier.height(8.dp))
        BigButton("حفظ الخطة") {
            val pm = pmt.toDoubleOrNull() ?: return@BigButton
            val nn = n.toIntOrNull() ?: return@BigButton
            if (name.isBlank() || pfId.isBlank()) return@BigButton
            onMutate { st ->
                st.plans.add(PayPlan(uid(), name.trim(), pfId, "", pm, nn, start,
                    st.pfs.firstOrNull { it.id == pfId }?.cur ?: "EGP"))
            }
            name = ""; pmt = ""; n = ""
        }
    }
}

// ---------- الحرية المالية ----------

@Composable
private fun FreedomTab(s: AppState, onMutate: ((AppState) -> Unit) -> Unit) {
    val t = LocalTheme
    val tot = Finance.totals(s)
    var years by remember { mutableStateOf("10") }
    var rate by remember { mutableStateOf("12") }
    var monthlyAdd by remember { mutableStateOf("") }

    Card2 {
        SectionTitle("الحرية المالية")
        Spacer(Modifier.height(6.dp))
        Muted("احسب قيمة محفظتك بعد سنوات من الاستثمار الشهري المستمر", size = 11)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(years, { years = it }, "عدد السنوات", Modifier.weight(1f))
            Field(rate, { rate = it }, "عائد سنوي %", Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Field(monthlyAdd, { monthlyAdd = it }, "إضافة شهرية (اختياري)")
        Spacer(Modifier.height(8.dp))
        val y = years.toIntOrNull() ?: 0
        val r = (rate.toDoubleOrNull() ?: 0.0) / 100
        val add = monthlyAdd.toDoubleOrNull() ?: 0.0
        var fv = tot.value * Math.pow(1 + r, y.toDouble())
        if (add > 0 && r > 0) fv += add * ((Math.pow(1 + r / 12, y * 12.0) - 1) / (r / 12))
        else if (add > 0) fv += add * 12 * y
        Text("${fmt(fv, 0)} ج", color = t.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Muted("قيمة متوقعة بعد $y سنة بعائد ${pc(rate.toDoubleOrNull() ?: 0.0)}", size = 11)
        Spacer(Modifier.height(6.dp))
        Row3(
            "القيمة الآن" to "${fmt(tot.value, 0)} ج",
            "مضاعف" to if (tot.value > 0) "${fmt(fv / tot.value, 1)}×" else "—",
            "الربح المتوقع" to "${fmt((fv - tot.value - add * 12 * y).coerceAtLeast(0.0), 0)} ج",
        )
    }
}

// ---------- أهداف مستقبلية ----------

@Composable
private fun GoalsTab(s: AppState, onMutate: ((AppState) -> Unit) -> Unit) {
    val t = LocalTheme
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var by by remember { mutableStateOf(today()) }

    Card2 {
        SectionTitle("أهداف مستقبلية (${s.goals.size})")
        Spacer(Modifier.height(6.dp))
        s.goals.forEach { g ->
            val progress = if (g.target > 0) (g.saved / g.target).coerceIn(0.0, 1.0) else 0.0
            Column(Modifier.fillMaxWidth().background(t.card2, chipShape).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Muted("الهدف: ${fmt(g.target, 0)} ${Finance.curSym(g.cur)} بحلول ${g.by}", size = 10)
                    }
                    Text("حذف", color = t.red, fontSize = 11.sp, modifier = Modifier.clickable {
                        onMutate { st -> st.goals.removeAll { it.id == g.id } }
                    })
                }
                Spacer(Modifier.height(4.dp))
                Bar(progress.toFloat(), "#0e9f6e")
                Muted("محقق ${pc(progress * 100, 0)} — موفر ${fmt(g.saved, 0)}", size = 10)
            }
            Spacer(Modifier.height(6.dp))
        }
        if (s.goals.isEmpty()) Muted("لا توجد أهداف بعد. أضف هدفًا وابدأ التوفير.", size = 11)

        Spacer(Modifier.height(10.dp))
        SectionTitle("إضافة هدف")
        Field(name, { name = it }, "اسم الهدف")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(target, { target = it }, "المبلغ المطلوب", Modifier.weight(1f))
            Field(by, { by = it }, "بحلول (yyyy-MM-dd)", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        BigButton("حفظ الهدف") {
            val tg = target.toDoubleOrNull() ?: return@BigButton
            if (name.isBlank()) return@BigButton
            onMutate { st -> st.goals.add(Goal(uid(), name.trim(), tg, "EGP", by)) }
            name = ""; target = ""
        }
    }
}
