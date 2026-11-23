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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.calc.Finance.fmt
import com.mizanati.invest.calc.Finance.pc
import com.mizanati.invest.data.AppState
import com.mizanati.invest.data.Portfolio

// Home (المحافظ) — control-panel hero card, 6 KPI stat cards, net-fx profit card,
// rotating advice banner, portfolios entry + emergency + analysis center.

private val tips = listOf(
    Triple("✨", "فَكّر في الاستثمار كمملكة طويلة الأجل، لا كضغطة قصيرة.", "مستوحى من رسالة وارن بافيت للمساهمين، 1996"),
    Triple("✨", "الصبر جزء من الخطط؛ لا تُدع تقلبات اليوم تقرر مستقبل محفظتك.", "مستوحى من مبادئ التشارك المنضبط في الأسبر"),
    Triple("✨", "وزّع محفظتك بين الدولار والجنيه بحسب هدفك لا بحسب أخبار اليوم.", "قاعدة توزيع العملات في التخطيط المالي"),
    Triple("✨", "راجع أدواتك كل فترة، وقِس العائد الحقيقي بالـ XIRR لا بالإحساس.", "ميزانيتي · أدوات القياس"),
    Triple("✨", "ابنِ صندوق طوارئ يكفي 3–6 أشهر قبل أي استثمار جديد.", "قاعدة الأمان المالي"),
)

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
    var tipIx by remember { mutableIntStateOf(0) }

    // fx effects aggregated across portfolios, split by currency (like the original dashboard)
    var fxUsd = 0.0; var fxEgp = 0.0
    s.pfs.filter { it.showHome && !it.isEmergency }.forEach { p ->
        val st = Finance.pfStats(p, s.ins, s.fx.now)
        if (p.cur == "USD") fxUsd += st.fxEffect else fxEgp += st.fxEffect
    }
    val netFx = fxUsd + fxEgp / s.fx.now

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ==== hero control panel ====
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(Color(0xFFEAF0FF), t.card)), bigShape,
                ).border(1.dp, t.border, bigShape).padding(18.dp),
            ) {
                Text(
                    "لوحة تتحكم بجميع المحافظ",
                    color = t.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(t.card2, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp).align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$${fmt(tot.valueUsd, 0)}",
                    color = t.text, fontSize = 40.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "≈ ج${fmt(tot.value, 0)} بسعر ${fmt(s.fx.now, 2)}",
                    color = t.muted, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(14.dp))
                // 2 rows × 3 stat cards
                StatRow(
                    listOf(
                        "أثر الصرف الدولار (ج)" to fmt(fxUsd, 0),
                        "أثر الصرف الجنيه ($)" to fmt(fxEgp, 0),
                        "نسبة الربح" to pc(tot.pct),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                StatRow(
                    listOf(
                        "XIRR الإجمالي (بسعر الجملة الحقيقي)" to (if (tot.xirr.isNaN()) "—" else pc(tot.xirr)),
                        "إجمالي الربح (محقق + متوقع)" to "$${fmt(tot.profit / s.fx.now, 0)}",
                        "سعر الصرف" to fmt(s.fx.now, 2),
                    ),
                )
                Spacer(Modifier.height(12.dp))
                // net fx profit card
                Row(
                    Modifier.fillMaxWidth().background(t.card, bigShape).border(1.dp, t.border, bigShape).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💱", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "صافي ربح الصرف (مكسب الدولاري − خسارة الجنيهي)",
                                color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$${fmt(netFx, 0)}",
                            color = if (netFx >= 0) t.green else t.red, fontSize = 26.sp, fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        "ج${fmt(netFx * s.fx.now, 0)} ≈",
                        color = t.muted, fontSize = 13.sp,
                        modifier = Modifier.background(t.card2, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // ==== rotating advice banner ====
            Row(
                Modifier.fillMaxWidth().background(
                    Brush.horizontalGradient(listOf(Color(0xFF3B2FA3), Color(0xFF1F8FB4))), RoundedCornerShape(18.dp),
                ).clickable { tipIx = (tipIx + 1) % tips.size }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tips[tipIx].first + " استثمر بوعي", color = Color(0xFFFFD75E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(tips[tipIx].second, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(tips[tipIx].third, color = Color(0xB3FFFFFF), fontSize = 10.sp)
                }
                Text(
                    "↗", color = Color.White, fontSize = 20.sp,
                    modifier = Modifier.background(Color(0x33FFFFFF), CircleShape).padding(10.dp),
                )
            }

            // ==== portfolios entry ====
            PortfolioEntry("🏦", "المحافظ الاستثمارية",
                "${s.pfs.count { !it.isEmergency }} محافظ · اضغط لفتح القائمة كاملة") {
                s.pfs.firstOrNull { !it.isEmergency }?.let { onOpenPortfolio(it.id) }
            }
            if (ef.include) PortfolioEntry("🛟", "المحافظ الاستثمارية — صندوق الطوارئ",
                "الرصيد: ${fmt(efBal, 0)} ${Finance.curSym(ef.currency)}", onClick = onOpenEmergency)

            // ==== analysis center ====
            PortfolioEntry("✦", "مركز التحليل", "المؤشرات الاستثمارية وأدوات التخطيط المالي") { onOpenAnalysis("xirr") }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalysisTile("📈", "الأداء والعوائد", "XIRR وربح كل أداة ومحفظة") { onOpenAnalysis("xirr") }
                AnalysisTile("🥧", "توزيع المحافظ", "حصة كل محفظة من القيمة") { onOpenAnalysis("pf") }
                AnalysisTile("💱", "توزيع العملات", "نسبة الدولار مقابل المستهدف ${s.split}%") { onOpenAnalysis("cur") }
                AnalysisTile("🗂", "بيانات الأدوات", "جدول كل الأدوات بعملتين") { onOpenAnalysis("all") }
                AnalysisTile("🧭", "بوصلة التركيز", "تركّز أكبر الأدوات ومخاطره") { onOpenAnalysis("focus") }
                AnalysisTile("📊", "نمو العوائد", "تطور القيمة والأرباح شهريًا") { onOpenAnalysis("growth") }
            }

            Muted("MIZANATI INVEST · v1.2.5", size = 11, modifier = Modifier.align(Alignment.CenterHorizontally))
            if (fxMsg.isNotBlank()) Muted(fxMsg, size = 11)
        }
        // floating +
        Text(
            "+", color = t.primary, fontSize = 26.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)
                .background(t.card, CircleShape).border(1.dp, t.border, CircleShape)
                .clickable { s.pfs.firstOrNull { !it.isEmergency }?.let { onOpenPortfolio(it.id) } }
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun StatRow(items: List<Pair<String, String>>) {
    val t = LocalTheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, v) ->
            Column(
                Modifier.weight(1f).background(t.card, chipShape).border(1.dp, t.border, chipShape).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(k, color = t.muted, fontSize = 10.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(v, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PortfolioEntry(icon: String, title: String, sub: String, onClick: () -> Unit) {
    val t = LocalTheme
    Row(
        Modifier.fillMaxWidth().background(t.card, RoundedCornerShape(18.dp))
            .border(1.dp, t.border, RoundedCornerShape(18.dp))
            .clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.background(t.card2, RoundedCornerShape(12.dp)).padding(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = t.muted, fontSize = 12.sp)
        }
        Text("←", color = t.primary, fontSize = 18.sp)
    }
}

@Composable
fun Ring(v: Float, colorHex: String, sizePx: Float = 52f) {
    val t = LocalTheme
    val col = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { t.primary }
    androidx.compose.foundation.Canvas(Modifier.size(sizePx.dp)) {
        drawArc(color = t.border.copy(alpha = .5f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
        drawArc(color = col, startAngle = -90f, sweepAngle = 360f * v.coerceIn(0f, 1f), useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
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

val bigShape = RoundedCornerShape(20.dp)
