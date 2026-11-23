package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.AppState

// Tools & analytics (الأدوات): XIRR per portfolio, allocation, growth view — mirrors the original tools tab.

@Composable
fun ToolsScreen(s: AppState) {
    val t = LocalTheme
    val toEgp: (Double, String) -> Double = { v, cur -> if (cur == "USD") v * s.fx.now else v }

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("الأدوات والتحليلات")
        Muted("مؤشرات الأداء لكل محفظة على عملة موحدة (الجنيه).", size = 12)

        s.pfs.forEach { p ->
            val st = Finance.pfStats(p, s.ins, s.fx.now, toEgp)
            Card2 {
                Text(p.name, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row3(
                    "قيمة" to "${Finance.fmt(st.value, 0)} ج",
                    "ربح" to (if (st.profitPct.isNaN()) "—" else Finance.pc(st.profitPct)),
                    "XIRR" to (if (st.xirr.isNaN()) "—" else Finance.pc(st.xirr)),
                )
            }
        }

        // allocation by portfolio
        val total = Finance.totalValue(s, toEgp)
        if (total > 0) {
            Card2 {
                SectionTitle("التوزيع")
                Spacer(Modifier.height(8.dp))
                s.pfs.filter { it.showHome }.forEach { p ->
                    val v = toEgp(s.ins.filter { it.pf == p.id }.sumOf { Finance.currentValue(it) }, p.cur)
                    val share = v / total * 100
                    Muted("${p.name}: ${Finance.pc(share)}", size = 12)
                }
            }
        }
    }
}
