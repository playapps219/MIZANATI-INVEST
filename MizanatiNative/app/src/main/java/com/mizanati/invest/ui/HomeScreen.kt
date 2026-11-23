package com.mizanati.invest.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.calc.Finance.fmt
import com.mizanati.invest.calc.Finance.pc
import com.mizanati.invest.data.AppState

// Home (الملخص) — top KPI cards, portfolio cards with progress rings, analysis center,
// fx impact card, emergency fund panel. Mirrors the original dashboard.

@Composable
fun HomeScreen(
    s: AppState,
    fxMsg: String,
    onRefreshFx: () -> Unit,
    onOpenPortfolio: (String) -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenAnalysis: (String) -> Unit,
) {
    val t = LocalTheme
    val tot = Finance.totals(s)
    val ef = s.emergencyFund
    val efBal = Finance.efBalance(ef)

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ==== hero totals ====
        Card2 {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Muted("إجمالي القيمة الحالية", size = 11)
                    Text("${fmt(tot.value, 0)} ج", color = t.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    val profitTxt = "${if (tot.profit >= 0) "+" else ""}${fmt(tot.profit, 0)} ج (${pc(tot.pct)})"
                    Text(profitTxt, color = if (tot.profit >= 0) t.green else t.red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Muted("إجمالي المستثمر", size = 11)
                    Text("${fmt(tot.cost, 0)} ج", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Muted("بالدولار ≈ $${fmt(tot.valueUsd, 0)}", size = 11)
                }
            }
            if (!tot.xirr.isNaN()) {
                Spacer(Modifier.height(6.dp))
                Muted("العائد السنوي المركب XIRR: ${pc(tot.xirr)}", size = 12)
            }
        }

        // ==== FX card ====
        Card2 {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Muted("سعر الدولار الحالي", size = 11)
                    Text("${fmt(s.fx.now, 2)} ج", color = t.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Muted(if (s.fx.upd.isNotBlank()) "آخر تحديث: ${s.fx.upd}" else "لم يتم التحديث بعد", size = 11)
                }
                Text(
                    "تحديث", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(t.primary, chipShape).clickable { onRefreshFx() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
            if (fxMsg.isNotBlank()) { Spacer(Modifier.height(6.dp)); Muted(fxMsg, size = 11) }
            if (s.fx.book > 0) {
                val chg = (s.fx.now - s.fx.book) / s.fx.book * 100
                Spacer(Modifier.height(6.dp))
                Text(
                    "منذ سعر البدء (${fmt(s.fx.book, 2)} ج بتاريخ ${s.fx.bookDate}): ${if (chg >= 0) "+" else ""}${pc(chg)}",
                    color = if (chg >= 0) t.green else t.red, fontSize = 12.sp,
                )
            }
        }

        // ==== portfolio cards ====
        s.pfs.filter { it.showHome && !it.isEmergency }.forEach { p ->
            val st = Finance.pfStats(p, s.ins, s.fx.now)
            val target = p.tAmt
            val progress = if (target > 0) (st.value / target).coerceIn(0.0, 1.0).toFloat() else 0f
            Card2(Modifier.clickable { onOpenPortfolio(p.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(14.dp).background(
                            try { Color(android.graphics.Color.parseColor(p.col)) } catch (e: Exception) { t.primary },
                            CircleShape,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Muted(if (p.cur == "USD") "بالدولار · ${fmt(st.value / s.fx.now, 0)} $" else "بالجنيه", size = 11)
                    }
                    if (target > 0) Ring(progress, p.col, 52f)
                }
                Spacer(Modifier.height(8.dp))
                Row3(
                    "المستثمر" to "${fmt(st.cost, 0)} ${Finance.curSym(p.cur)}",
                    "القيمة" to "${fmt(st.value, 0)} ${Finance.curSym(p.cur)}",
                    "الربح" to (if (st.total.isNaN()) "—" else "${fmt(st.total, 0)} ${Finance.curSym(p.cur)}"),
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Muted("XIRR: ${pc(st.xr)}", size = 11)
                    if (target > 0) Muted("الهدف: ${fmt(target, 0)} (${pc(st.value / target * 100, 0)})", size = 11)
                    if (!st.fxEffect.isNaN() && kotlin.math.abs(st.fxEffect) > 1) {
                        Text(
                            "أثر الصرف: ${fmt(st.fxEffect, 0)} ${Finance.curSym(p.cur)}",
                            color = if (st.fxEffect >= 0) t.green else t.red,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        // ==== emergency fund ====
        if (ef.include) {
            Card2(Modifier.clickable { onOpenEmergency() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛟", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("صندوق الطوارئ", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Muted("الرصيد: ${fmt(efBal, 0)} ${Finance.curSym(ef.currency)}", size = 11)
                    }
                    val x = Finance.efXirr(ef)
                    if (!x.isNaN()) Text(pc(x), color = t.green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==== analysis center tiles ====
        SectionTitle("مركز التحليلات")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalysisTile("📈", "الأداء والعوائد", "XIRR وربح كل أداة ومحفظة") { onOpenAnalysis("xirr") }
            AnalysisTile("🥧", "توزيع المحافظ", "حصة كل محفظة من القيمة") { onOpenAnalysis("pf") }
            AnalysisTile("💱", "توزيع العملات", "نسبة الدولار مقابل المستهدف ${s.split}%") { onOpenAnalysis("cur") }
            AnalysisTile("🗂", "بيانات الأدوات", "جدول كل الأدوات بعملتين") { onOpenAnalysis("all") }
            AnalysisTile("🧭", "بوصلة التركيز", "تركّز أكبر الأدوات ومخاطره") { onOpenAnalysis("focus") }
            AnalysisTile("📊", "نمو العوائد", "تطور القيمة والأرباح شهريًا") { onOpenAnalysis("growth") }
        }

        Muted("MIZANATI INVEST · v1.2.5", size = 11, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun Ring(v: Float, colorHex: String, sizePx: Float = 52f) {
    val t = LocalTheme
    val col = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { t.primary }
    androidx.compose.foundation.Canvas(Modifier.size(sizePx.dp)) {
        drawArc(color = t.border.copy(alpha = .5f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawArc(color = col, startAngle = -90f, sweepAngle = 360f * v.coerceIn(0f, 1f), useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun AnalysisTile(icon: String, title: String, sub: String, onClick: () -> Unit) {
    val t = LocalTheme
    Row(
        Modifier.fillMaxWidth().background(t.card, cardShape).border(1.dp, t.border, cardShape)
            .clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = t.muted, fontSize = 11.sp)
        }
        Text("‹", color = t.muted, fontSize = 20.sp)
    }
}
