package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.mizanati.invest.data.AppState

// Home (الملخص) screen: totals, fx card, portfolio bars, emergency fund — mirrors the original dashboard.

@Composable
fun HomeScreen(
    s: AppState,
    fxMsg: String,
    onRefreshFx: () -> Unit,
    onOpenPortfolio: (String) -> Unit,
    onGoTools: () -> Unit,
) {
    val t = LocalTheme
    val toEgp: (Double, String) -> Double = { v, cur ->
        if (cur == "USD") v * s.fx.now else v
    }
    val invested = Finance.totalInvested(s, toEgp)
    val value = Finance.totalValue(s, toEgp)
    val profit = value - invested
    val pct = if (invested > 0) profit / invested * 100 else Double.NaN
    val fxDown = if (s.fx.book > 0) (s.fx.now - s.fx.book) / s.fx.book * 100 else Double.NaN

    Column(
        Modifier
            .fillMaxSize()
            .background(t.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // FX card
        Card2 {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Muted("سعر الدولار الحالي", size = 11)
                    Text("${Finance.fmt(s.fx.now, 2)} ج", color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Muted(if (s.fx.upd.isNotBlank()) "آخر تحديث: ${s.fx.upd}" else "لم يتم التحديث بعد", size = 11)
                }
                Text(
                    "تحديث",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(t.primary, chipShape)
                        .clickable { onRefreshFx() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
            if (fxMsg.isNotBlank()) {
                Spacer(Modifier.height(6.dp)); Muted(fxMsg, size = 11)
            }
            if (!fxDown.isNaN()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "تغير الدولار منذ الشراء: ${Finance.pc(fxDown)}",
                    color = if (fxDown >= 0) t.green else t.red, fontSize = 12.sp,
                )
            }
        }

        // totals
        Card2 {
            SectionTitle("إجمالي المحفظة")
            Spacer(Modifier.height(8.dp))
            Row3(
                "المستثمر" to "${Finance.fmt(invested, 0)} ج",
                "القيمة الحالية" to "${Finance.fmt(value, 0)} ج",
                "الربح" to "${Finance.fmt(profit, 0)} ج",
            )
            Spacer(Modifier.height(8.dp))
            ProfitText(profit, pct)
        }

        // portfolios
        Card2 {
            SectionTitle("المحافظ")
            Spacer(Modifier.height(8.dp))
            s.pfs.filter { it.showHome }.forEach { p ->
                val st = Finance.pfStats(p, s.ins, s.fx.now, toEgp)
                Row(
                    Modifier.fillMaxWidth()
                        .background(t.card2, chipShape)
                        .border(1.dp, t.border, chipShape)
                        .clickable { onOpenPortfolio(p.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(12.dp).background(
                            try { Color(android.graphics.Color.parseColor(p.col)) } catch (e: Exception) { t.primary },
                            CircleShape,
                        ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Muted("قيمة ${Finance.fmt(st.value, 0)} ج · ربح ${Finance.fmt(st.profit, 0)} ج", size = 11)
                    }
                    Text(
                        if (st.profitPct.isNaN()) "—" else Finance.pc(st.profitPct),
                        color = if (st.profit >= 0) t.green else t.red,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // emergency fund summary
        if (s.emergencyFund.include) {
            Card2 {
                SectionTitle("صندوق الطوارئ")
                Spacer(Modifier.height(6.dp))
                val ef = s.emergencyFund
                val total = ef.transactions.sumOf { it.amt }
                Text("${Finance.fmt(total, 0)} ${ef.currency}", color = t.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Muted("MIZANATI INVEST · v1.2.5", size = 11, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
