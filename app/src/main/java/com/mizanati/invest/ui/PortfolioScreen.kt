package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.*

// Wallet detail (المحفظة): instruments list, add instrument, add transaction, stats.

@Composable
fun PortfolioScreen(
    s: AppState,
    pfId: String,
    onMutate: ((AppState) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val t = LocalTheme
    val p = s.pfs.firstOrNull { it.id == pfId } ?: return
    val toEgp: (Double, String) -> Double = { v, cur -> if (cur == "USD") v * s.fx.now else v }
    val mine = s.ins.filter { it.pf == p.id }
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var txAmt by remember { mutableStateOf("") }
    var txDate by remember { mutableStateOf(today()) }
    var targetIns by remember { mutableStateOf<Instrument?>(null) }

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = t.primary, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            SectionTitle(p.name)
        }

        val st = Finance.pfStats(p, s.ins, s.fx.now, toEgp)
        Card2 {
            Row3(
                "المستثمر" to Finance.fmt(st.invested, 0) + " ج",
                "القيمة" to Finance.fmt(st.value, 0) + " ج",
            )
            Spacer(Modifier.height(8.dp))
            ProfitText(st.profit, st.profitPct)
            if (!st.xirr.isNaN()) {
                Spacer(Modifier.height(4.dp))
                Muted("العائد السنوي المركب XIRR: ${Finance.pc(st.xirr)}", size = 12)
            }
        }

        Card2 {
            SectionTitle("الأدوات")
            Spacer(Modifier.height(8.dp))
            if (mine.isEmpty()) Muted("لا توجد أدوات بعد. أضف أول أداة من الزر بالأسفل.", size = 12)
            mine.forEach { i ->
                val valEgp = toEgp(Finance.currentValue(i), p.cur)
                val cost = toEgp(i.tx.sumOf { it.amt }, p.cur)
                Row(
                    Modifier.fillMaxWidth()
                        .background(t.card2, chipShape)
                        .clickable { targetIns = i }
                        .padding(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(i.name, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (i.kind.isNotBlank()) Muted(i.kind, size = 11)
                        Muted("تكلفة ${Finance.fmt(cost, 0)} ج · قيمة ${Finance.fmt(valEgp, 0)} ج", size = 11)
                    }
                    val d = valEgp - cost
                    Text(
                        Finance.fmt(d, 0) + " ج",
                        color = if (d >= 0) t.green else t.red,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            BigButton(if (showAdd) "إلغاء" else "+ إضافة أداة") { showAdd = !showAdd }
            if (showAdd) {
                Spacer(Modifier.height(8.dp))
                Field(name, { name = it }, "اسم الأداة")
                Spacer(Modifier.height(8.dp))
                BigButton("حفظ الأداة") {
                    if (name.isNotBlank()) onMutate { st2 ->
                        st2.ins.add(Instrument(id = uid(), pf = p.id, name = name.trim()))
                        name = ""; showAdd = false
                    }
                }
            }
        }

        targetIns?.let { ins ->
            Card2 {
                SectionTitle("حركة على: ${ins.name}")
                Spacer(Modifier.height(8.dp))
                ins.tx.forEach { tx ->
                    Muted("${tx.d} · ${Finance.fmt(tx.amt, 2)} ${p.cur}", size = 12)
                }
                Spacer(Modifier.height(8.dp))
                Field(txDate, { txDate = it }, "التاريخ (yyyy-MM-dd)")
                Spacer(Modifier.height(6.dp))
                Field(txAmt, { txAmt = it }, "المبلغ (+ إيداع / − سحب)")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("إيداع", Modifier.weight(1f)) {
                        val a = txAmt.toDoubleOrNull() ?: return@BigButton
                        onMutate { st2 ->
                            st2.ins.find { it.id == ins.id }?.tx?.add(
                                Tx(id = uid(), d = txDate, amt = a, fx = if (p.cur == "USD") s.fx.now else 0.0),
                            )
                        }
                        txAmt = ""
                    }
                    BigButton("تحديث القيمة", Modifier.weight(1f)) {
                        val a = txAmt.toDoubleOrNull() ?: return@BigButton
                        onMutate { st2 ->
                            st2.ins.find { it.id == ins.id }?.apply { cv = a; cvDate = txDate }
                        }
                        txAmt = ""
                    }
                }
            }
        }
    }
}
