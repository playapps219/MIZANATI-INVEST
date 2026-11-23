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

// Analysis detail screen (بلوكات مركز التحليل): xirr | pf | cur | all | focus | growth.

@Composable
fun AnalysisScreen(s: AppState, mode: String, onBack: () -> Unit) {
    val t = LocalTheme
    val titles = mapOf(
        "xirr" to ("📈" to "الأداء والعوائد"),
        "pf" to ("🥧" to "توزيع المحافظ"),
        "cur" to ("💱" to "توزيع العملات"),
        "all" to ("🗂" to "بيانات الأدوات"),
        "focus" to ("🧭" to "بوصلة التركيز"),
        "growth" to ("📊" to "نمو العوائد"),
    )
    val (icon, title) = titles[mode] ?: ("📈" to "التحليل")
    val tot = Finance.totals(s)

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = t.primary, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(10.dp))
            Text("$icon $title", color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        when (mode) {
            "xirr" -> {
                s.pfs.filter { it.showHome && !it.isEmergency }.forEach { p ->
                    val st = Finance.pfStats(p, s.ins, s.fx.now)
                    Card2 {
                        Text(p.name, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row3(
                            "XIRR" to pc(st.xr),
                            "نسبة الربح" to pc(st.pct),
                            "الربح" to "${fmt(st.total, 0)} ${Finance.curSym(p.cur)}",
                        )
                        if (p.tRet > 0) {
                            Spacer(Modifier.height(4.dp))
                            val ok = !st.xr.isNaN() && st.xr * 100 >= p.tRet
                            Text(
                                "الهدف: ${pc(p.tRet)} — ${if (ok) "متجاوز ✓" else "تحت الهدف"}",
                                color = if (ok) t.green else t.red, fontSize = 12.sp,
                            )
                        }
                    }
                }
                s.ins.forEach { i ->
                    val p = s.pfs.firstOrNull { it.id == i.pf } ?: return@forEach
                    val st = Finance.insStats(i, s.fx.now)
                    if (st.cost > 0) Card2 {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(i.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Muted("${p.name} · تكلفة ${fmt(st.cost, 0)}", size = 10)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(pc(st.xr), color = if (st.xr >= 0) t.green else t.red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Muted("ربح ${fmt(st.total, 0)}", size = 10)
                            }
                        }
                    }
                }
            }

            "pf" -> {
                val per = s.pfs.filter { it.showHome && !it.isEmergency }.map { p ->
                    val v = s.ins.filter { it.pf == p.id }.sumOf { Finance.insStats(it, s.fx.now).value }
                    val ve = if (p.cur == "USD") v * s.fx.now else v
                    Triple(p, v, if (tot.value > 0) ve / tot.value * 100 else 0.0)
                }
                Card2 {
                    SectionTitle("حصص المحافظ")
                    per.forEach { (p, v, share) ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(
                                try { Color(android.graphics.Color.parseColor(p.col)) } catch (e: Exception) { t.primary },
                                androidx.compose.foundation.shape.CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Bar(share.toFloat() / 100f, p.col)
                            }
                            Text("${pc(share, 1)}", color = t.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Muted("${fmt(v, 0)} ${Finance.curSym(p.cur)}", size = 10)
                    }
                }
            }

            "cur" -> {
                Card2 {
                    SectionTitle("توزيع العملات")
                    Spacer(Modifier.height(6.dp))
                    Text("نسبة الدولار: ${pc(tot.usdPct * 100, 1)}", color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("المستهدف: ${s.split}% دولار / ${100 - s.split}% جنيه", color = t.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().height(24.dp)) {
                        Box(Modifier.weight(tot.usdPct.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFF2f6df6)))
                        Box(Modifier.weight((1f - tot.usdPct.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFF0e9f6e)))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted("دولار: ${fmt(tot.usdVal, 0)} $", size = 11)
                        Muted("جنيه: ${fmt(tot.egpVal - tot.usdVal * s.fx.now, 0)} ج", size = 11)
                    }
                    val dev = (tot.usdPct * 100 - s.split)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (kotlin.math.abs(dev) <= 5) "توزيع العملات قريب من المستهدف ✓" else
                            "انحراف الدولار: ${if (dev > 0) "+" else ""}${fmt(dev, 1)} نقطة عن المستهدف",
                        color = if (kotlin.math.abs(dev) <= 5) t.green else t.red, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }

            "all" -> {
                Card2 {
                    SectionTitle("جدول كل الأدوات (بجدنيه وبالدولار)")
                    Spacer(Modifier.height(8.dp))
                    s.ins.forEach { i ->
                        val p = s.pfs.firstOrNull { it.id == i.pf } ?: return@forEach
                        val st = Finance.insStats(i, s.fx.now)
                        val vE = if (p.cur == "USD") st.value * s.fx.now else st.value
                        val vU = if (p.cur == "USD") st.value else st.value / s.fx.now
                        Column(Modifier.fillMaxWidth().background(t.card2, chipShape).padding(10.dp)) {
                            Text(i.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Muted("${p.name} · ${p.secs.firstOrNull { it.id == i.sec }?.name ?: i.kind}", size = 10)
                            Spacer(Modifier.height(4.dp))
                            Row3(
                                "تكلفة" to "${fmt(if (p.cur == "USD") st.cost * s.fx.now else st.cost, 0)} ج",
                                "قيمة" to "${fmt(vE, 0)} ج",
                                "ربح" to pc(if (st.cost > 0) (vE - st.cost * (if (p.cur == "USD") s.fx.now else 1.0)) / (st.cost * (if (p.cur == "USD") s.fx.now else 1.0)) * 100.0 else Double.NaN),
                            )
                            Spacer(Modifier.height(4.dp))
                            Row3(
                                "قيمة $" to "${fmt(vU, 0)} $",
                                "XIRR" to pc(st.xr),
                                "العمر" to "${st.ageDays} يوم",
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            "focus" -> {
                val live = s.ins.map { i -> i to Finance.insStats(i, s.fx.now) }
                    .filter { (_, st) -> st.value > 0 }
                    .map { (i, st) -> Triple(i, st, st.value) }
                val totV = live.sumOf { it.third }
                val sorted = live.sortedByDescending { it.third }
                val hhi = live.sumOf { (it.third / (totV.coerceAtLeast(1.0))).pow(2.0) }
                Card2 {
                    SectionTitle("التركّز — أكبر ${minOf(5, live.size)} أدوات")
                    Muted("مؤشر التركّز ${fmt(hhi * 100, 0)} (${if (hhi > .4) "⚠️ مرتفع" else if (hhi > .25) "متوسط" else "منخفض"}) — النسبة من إجمالي ثروتك", size = 11)
                    Spacer(Modifier.height(8.dp))
                    sorted.take(5).forEachIndexed { k, (i, st, v) ->
                        val pct = if (totV > 0) v / totV * 100 else 0.0
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${k + 1}", color = t.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text(i.name, color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Bar((pct / 100.0).toFloat(), "#635bff")
                            }
                            Text(pc(pct, 1), color = t.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "growth" -> {
                Card2 {
                    SectionTitle("نمو القيمة والأرباح شهريًا")
                    Muted("محسوب من أحدث قيمة مسجلة لكل أداة عبر الأشهر", size = 11)
                    Spacer(Modifier.height(10.dp))
                    // simple month bars based on tx activity
                    val months = 6
                    var maxM = 1.0
                    val vals = (months - 1 downTo 0).map { back ->
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.MONTH, -back)
                        val key = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(cal.time)
                        val v = s.ins.sumOf { i ->
                            i.tx.filter { it.d.startsWith(key) }.sumOf { kotlin.math.abs(it.amt) }
                        }
                        key to v
                    }
                    vals.forEach { v -> if (v.second > maxM) maxM = v.second }
                    Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vals.forEach { (key, v) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height((40 + 80 * (v / maxM)).dp).background(t.primary, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)))
                                Spacer(Modifier.height(4.dp))
                                Muted(key.substring(5), size = 10)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun Double.pow(n: Double) = Math.pow(this, n)

@Composable
fun Bar(f: Float, colorHex: String) {
    val t = LocalTheme
    val col = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { t.primary }
    Row(Modifier.fillMaxWidth().height(6.dp).background(t.border.copy(alpha = .4f), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f.coerceIn(0f, 1f)).background(col, androidx.compose.foundation.shape.RoundedCornerShape(3.dp)))
    }
}
