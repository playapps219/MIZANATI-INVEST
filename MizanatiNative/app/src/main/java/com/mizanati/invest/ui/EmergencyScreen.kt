package com.mizanati.invest.ui

import androidx.compose.foundation.*
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
import com.mizanati.invest.calc.Finance.fmt
import com.mizanati.invest.calc.Finance.pc
import com.mizanati.invest.data.AppState
import com.mizanati.invest.data.EmergencyFund
import com.mizanati.invest.data.today
import com.mizanati.invest.data.uid
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

// Emergency fund screen (صندوق الطوارئ): deposits/withdrawals, invested mode, safety.

@Composable
fun EmergencyScreen(s: AppState, onMutate: ((AppState) -> Unit) -> Unit, onBack: () -> Unit) {
    val t = LocalTheme
    val ef = s.emergencyFund
    var d: String by remember { mutableStateOf(today()) }
    var amt: String by remember { mutableStateOf("") }
    var note: String by remember { mutableStateOf("") }
    var months: String by remember { mutableStateOf(ef.safetyMonths.toString()) }
    var spend: String by remember { mutableStateOf(ef.monthlySpend.toString()) }
    var cvVal: String by remember { mutableStateOf(if (ef.currentValue > 0) ef.currentValue.toString() else "") }

    val bal = Finance.efBalance(ef)
    val dep = Finance.efDeposits(ef)
    val wth = Finance.efWithdrawals(ef)
    val x = Finance.efXirr(ef)
    val age = Finance.efWeightedAgeDays(ef)
    val target = ef.monthlySpend * ef.safetyMonths
    val covered = if (ef.monthlySpend > 0) bal / ef.monthlySpend else 0.0

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = t.primary, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(10.dp))
            Text("🛟 صندوق الطوارئ", color = t.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Card2 {
            Muted("الرصيد الحالي", size = 11)
            Text("${fmt(bal, 2)} ${Finance.curSym(ef.currency)}", color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (!x.isNaN()) Text("XIRR: ${pc(x)}", color = t.green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row3(
                "إيداعات" to fmt(dep, 0),
                "سحوبات" to fmt(wth, 0),
                "عمر مرجح" to "$age يوم",
            )
            if (ef.invested) {
                Spacer(Modifier.height(6.dp))
                Muted("الصندوق مستثمر — القيمة الحالية مسجلة يدويًا", size = 11)
            }
        }

        Card2 {
            SectionTitle("أمان الصندوق")
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(months, { months = it }, "أشهر الأمان", Modifier.weight(1f))
                Field(spend, { spend = it }, "مصروف شهري", Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text("الهدف: ${fmt(target, 0)} ${Finance.curSym(ef.currency)}", color = t.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            val pct = (covered * 100).coerceIn(0.0, 999.0)
            Spacer(Modifier.height(4.dp))
            Bar((pct / 100).toFloat(), if (covered >= 1) "#0e9f6e" else "#e5871a")
            Text(
                "التغطية: ${pc(pct, 0)} (${fmt(covered, 1)} شهر) — ${if (covered >= 1) "لا حاجة لإيداع ✓" else "أضف ${fmt((target - bal).coerceAtLeast(0.0), 0)} للوصول للأمان"}",
                color = if (covered >= 1) t.green else LocalOrange,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("حفظ", Modifier.weight(1f)) {
                    onMutate { st ->
                        st.emergencyFund.safetyMonths = months.toIntOrNull() ?: 3
                        st.emergencyFund.monthlySpend = spend.toDoubleOrNull() ?: 0.0
                    }
                }
                Chip(if (ef.invested) "مستثمر ✓" else "غير مستثمر", ef.invested) {
                    onMutate { st -> st.emergencyFund.invested = !st.emergencyFund.invested }
                }
            }
            if (ef.invested) {
                Spacer(Modifier.height(8.dp))
                Field(cvVal, { cvVal = it }, "القيمة الحالية للصندوق المستثمر")
                Spacer(Modifier.height(6.dp))
                BigButton("حفظ القيمة") {
                    val v = cvVal.toDoubleOrNull() ?: return@BigButton
                    onMutate { st ->
                        st.emergencyFund.currentValue = v
                        st.emergencyFund.currentValueDate = today()
                        st.emergencyFund.valueInitialized = true
                    }
                }
            }
        }

        Card2 {
            SectionTitle("الحركات (${ef.transactions.size})")
            Spacer(Modifier.height(8.dp))
            ef.transactions.sortedByDescending { it.d }.forEach { tx ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${tx.d} · ${fmt(tx.amt, 2)}", color = if (tx.amt >= 0) t.green else t.red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (tx.note.isNotBlank()) Muted(tx.note, size = 10)
                    }
                    Text("حذف", color = t.red, fontSize = 11.sp, modifier = Modifier.clickable {
                        onMutate { st -> st.emergencyFund.transactions.removeAll { it.id == tx.id } }
                    })
                }
                Spacer(Modifier.height(6.dp))
            }
            if (ef.transactions.isEmpty()) Muted("لا توجد حركات بعد", size = 11)
            Spacer(Modifier.height(10.dp))
            Field(d, { d = it }, "التاريخ (yyyy-MM-dd)")
            Spacer(Modifier.height(6.dp))
            Field(amt, { amt = it }, "المبلغ (سالب للسحب)")
            Spacer(Modifier.height(6.dp))
            Field(note, { note = it }, "ملاحظة (اختياري)")
            Spacer(Modifier.height(8.dp))
            BigButton("حفظ الحركة") {
                val a = amt.toDoubleOrNull() ?: return@BigButton
                onMutate { st -> st.emergencyFund.transactions.add(com.mizanati.invest.data.EfTx(uid(), d, a, note)) }
                amt = ""; note = ""
            }
        }
    }
}

val androidx.compose.ui.graphics.Color.orange: Color get() = Color(0xFFE5871A)

val LocalOrange = Color(0xFFE5871A)
