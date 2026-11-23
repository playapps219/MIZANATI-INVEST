package com.mizanati.invest.ui

import androidx.compose.foundation.*
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
import com.mizanati.invest.calc.Finance.fmt
import com.mizanati.invest.calc.Finance.pc
import com.mizanati.invest.data.AppState
import com.mizanati.invest.data.Instrument
import com.mizanati.invest.data.today
import com.mizanati.invest.data.uid

// Wallet detail screen (بطاقة المحفظة): tools table with XIRR/profit per tool,
// add/edit tool with all fields (kind-specific), transaction list with types.

@Composable
fun PortfolioScreen(
    s: AppState,
    pfId: String,
    onMutate: ((com.mizanati.invest.data.AppState) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val t = LocalTheme
    val p = s.pfs.firstOrNull { it.id == pfId } ?: return
    val mine = s.ins.filter { it.pf == p.id }
    val st = Finance.pfStats(p, s.ins, s.fx.now)
    var showAddIns by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newKind by remember { mutableStateOf("fund") }
    var newSec by remember { mutableStateOf("") }
    var showTxFor by remember { mutableStateOf<Instrument?>(null) }
    var txDate by remember { mutableStateOf(today()) }
    var txAmt by remember { mutableStateOf("") }
    var txUnits by remember { mutableStateOf("") }
    var txType by remember { mutableStateOf("buy") }
    var txNote by remember { mutableStateOf("") }
    var editCvFor by remember { mutableStateOf<Instrument?>(null) }
    var cvVal by remember { mutableStateOf("") }
    var editInstrument by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember(p.id) { mutableStateOf("") }
    var editingTx by remember { mutableStateOf<String?>(null) }
    var txError by remember { mutableStateOf("") }
    var deleteInstrument by remember { mutableStateOf<String?>(null) }

    val kinds = listOf(
        "fund" to "صندوق استثماري", "gold" to "ذهب", "cert" to "شهادات/وثائق",
        "stock" to "أسهم", "cash" to "نقد/دخل ثابت", "prop" to "عقار",
    )

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = t.primary, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            SectionTitle(p.name)
        }

        // ==== wallet hero card ====
        Card2 {
            Muted("القيمة الحالية ${if (p.cur == "USD") "بالدولار" else "بالجنيه"}", size = 11)
            Text("${fmt(st.value, 2)} ${Finance.curSym(p.cur)}", color = t.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "إجمالي الربح (محقق + متوقع): ${fmt(st.total, 2)} ${Finance.curSym(p.cur)}",
                color = if (st.total >= 0) t.green else t.red, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row3(
                "المستثمر" to "${fmt(st.cost, 0)}",
                "نسبة الربح" to pc(st.pct),
                "XIRR" to pc(st.xr),
            )
            Spacer(Modifier.height(6.dp))
            Row3(
                "ربح محقق" to "${fmt(st.realized, 0)}",
                "ربح متوقع" to "${fmt(st.unrealized, 0)}",
                "العمر المرجح" to "${st.ageDays} يوم",
            )
            if (kotlin.math.abs(st.fxEffect) > 0.5) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "أثر الصرف على محافظ ${if (p.cur == "USD") "الدولار (ج)" else "الجنيه ($)"}: ${fmt(st.fxEffect, 2)} ${Finance.curSym(if (p.cur == "USD") "EGP" else "USD")}",
                    color = if (st.fxEffect >= 0) t.green else t.red, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (p.tAmt > 0) {
                Spacer(Modifier.height(6.dp))
                Muted("هدف المحفظة: ${fmt(p.tAmt, 0)} ${Finance.curSym(p.cur)} · محقق ${pc(st.value / p.tAmt * 100, 0)} · العائد المستهدف ${pc(p.tRet)}", size = 11)
            }
        }

        PortfolioEditor(p, onMutate)
        editInstrument?.let { id ->
            s.ins.find { it.id == id }?.let { instrument ->
                InstrumentEditor(instrument, p, onMutate) { editInstrument = null }
            }
        }
        Card2 {
            SectionTitle("الأدوات (${mine.size})")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("جميع الأدوات", selectedSection.isBlank()) { selectedSection = "" }
                p.secs.forEach { sec ->
                    Chip("${sec.name} (${mine.count { it.sec == sec.id }})", selectedSection == sec.id) { selectedSection = sec.id }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (mine.isEmpty()) Muted("لا توجد أدوات بعد. أضف أول أداة بالزر بالأسفل.", size = 12)
            mine.filter { selectedSection.isBlank() || it.sec == selectedSection }.forEach { i ->
                val st2 = Finance.insStats(i, s.fx.now)
                Column(
                    Modifier.fillMaxWidth().background(t.card2, chipShape).padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { editCvFor = i }) {
                            Text(i.name, color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val kindLabel = kinds.firstOrNull { it.first == i.kind }?.second ?: i.kind
                            val secName = p.secs.firstOrNull { it.id == i.sec }?.name ?: i.sec
                            Muted("$kindLabel${if (secName.isNotBlank()) " · $secName" else ""}", size = 11)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${fmt(st2.value, 0)} ${Finance.curSym(p.cur)}", color = t.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                (if (st2.total >= 0) "+" else "") + fmt(st2.total, 0),
                                color = if (st2.total >= 0) t.green else t.red, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted("تكلفة ${fmt(st2.cost, 0)} · XIRR ${pc(st2.xr)}", size = 11)
                        Text("حركة وتحديث", color = t.primary, fontSize = 12.sp,
                            modifier = Modifier.clickable { showTxFor = i; editingTx = null; txError = "" })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("تعديل الأداة", editInstrument == i.id) { editInstrument = i.id }
                        Chip("القيمة الحالية", false) { editCvFor = i; cvVal = if (i.cvDate.isNotBlank()) i.cv.toString() else "" }
                        Chip("حذف", deleteInstrument == i.id) { deleteInstrument = if (deleteInstrument == i.id) null else i.id }
                    }
                    if (deleteInstrument == i.id) {
                        Muted("سيُحذف ${i.name} وجميع حركاته. لا يمكن التراجع عن ذلك.")
                        BigButton("تأكيد حذف الأداة", danger = true) {
                            onMutate { state -> state.ins.removeAll { it.id == i.id } }
                            if (showTxFor?.id == i.id) showTxFor = null
                            if (editInstrument == i.id) editInstrument = null
                            if (editCvFor?.id == i.id) editCvFor = null
                            deleteInstrument = null
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            BigButton(if (showAddIns) "إلغاء" else "+ إضافة أداة") { showAddIns = !showAddIns }
            if (showAddIns) {
                Spacer(Modifier.height(8.dp))
                Muted("نوع الأداة", size = 11)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    kinds.take(3).forEach { (k, label) ->
                        Chip(label, newKind == k, Modifier.weight(1f)) { newKind = k }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    kinds.drop(3).forEach { (k, label) ->
                        Chip(label, newKind == k, Modifier.weight(1f)) { newKind = k }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Field(newName, { newName = it }, "اسم الأداة")
                Spacer(Modifier.height(6.dp))
                if (p.secs.isNotEmpty()) {
                    Muted("القسم", size = 11)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.secs.forEach { sec ->
                            Chip(sec.name, newSec == sec.id) { newSec = sec.id }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                BigButton("حفظ الأداة") {
                    if (newName.isNotBlank()) onMutate { st3 ->
                        val instrument = Instrument(
                            id = uid(), pf = p.id, name = newName.trim(), kind = newKind,
                            sec = newSec.ifBlank { p.secs.firstOrNull()?.id ?: "" },
                        )
                        st3.ins.add(instrument)
                        editInstrument = instrument.id
                        newName = ""; newSec = ""; showAddIns = false
                    }
                }
            }
        }

        // ==== edit current value modal ====
        editCvFor?.let { ins ->
            Card2 {
                SectionTitle("تحديث القيمة الحالية: ${ins.name}")
                Spacer(Modifier.height(8.dp))
                Field(cvVal, { cvVal = it }, "القيمة الحالية (${Finance.curSym(p.cur)})")
                Spacer(Modifier.height(6.dp))
                Muted("اتركها فارغة للرجوع للاحتساب التلقائي", size = 10)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("حفظ", Modifier.weight(1f)) {
                        val v = cvVal.toDoubleOrNull()
                        onMutate { st3 ->
                            st3.ins.find { it.id == ins.id }?.apply {
                                cv = v ?: 0.0; cvDate = if (v != null) today() else ""
                            }
                        }
                        cvVal = ""; editCvFor = null
                    }
                    BigButton("إلغاء", Modifier.weight(1f)) { editCvFor = null; cvVal = "" }
                }
            }
        }

        // ==== transactions modal ====
        showTxFor?.let { ins ->
            Card2 {
                SectionTitle("حركات: ${ins.name}")
                Spacer(Modifier.height(8.dp))
                ins.tx.sortedByDescending { it.d }.forEach { tx ->
                    val typeLabel = mapOf(
                        "buy" to "شراء", "sell" to "بيع", "coupon" to "كوبون",
                        "maturity" to "استحقاق", "rent" to "إيجار", "fee" to "مصروف",
                    )[tx.typ] ?: tx.typ
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("$typeLabel · ${tx.d}", color = t.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (tx.note.isNotBlank()) Muted(tx.note, size = 10)
                            if (tx.units > 0) Muted("كمية: ${fmt(tx.units, 4)} · سعر: ${fmt(if (tx.amt > 0) tx.amt / tx.units else 0.0, 2)}", size = 10)
                            if (tx.fx > 0 && p.cur == "USD") Muted("دولار ${fmt(tx.fx, 2)}", size = 10)
                        }
                        Row {
                            Text("حذف", color = t.red, fontSize = 11.sp, modifier = Modifier.clickable {
                                onMutate { st3 -> st3.ins.find { it.id == ins.id }?.tx?.removeAll { it.id == tx.id } }
                            })
                            Spacer(Modifier.width(10.dp))
                            Text("تعديل", color = t.primary, fontSize = 11.sp, modifier = Modifier.clickable {
                                txDate = tx.d; txAmt = tx.amt.toString(); txUnits = if (tx.units > 0) tx.units.toString() else ""
                                txType = tx.typ; txNote = tx.note; editingTx = tx.id; txError = ""
                            })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (ins.tx.isEmpty()) Muted("لا توجد حركات بعد", size = 11)

                Spacer(Modifier.height(10.dp))
                SectionTitle("إضافة عملية جديدة")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("buy" to "شراء", "sell" to "بيع", "coupon" to "كوبون", "maturity" to "استحقاق").forEach { (k, l) ->
                        Chip(l, txType == k) { txType = k }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (ins.kind == "gold" || ins.kind == "cert") {
                    Field(txUnits, { txUnits = it }, "الكمية (${if (ins.kind == "gold") "جرام" else "وثيقة"})")
                    Spacer(Modifier.height(6.dp))
                }
                Field(txDate, { txDate = it }, "التاريخ (yyyy-MM-dd)")
                Spacer(Modifier.height(6.dp))
                Field(txAmt, { txAmt = it }, "المبلغ (${Finance.curSym(p.cur)}) — سالب للسحب")
                Spacer(Modifier.height(6.dp))
                Field(txNote, { txNote = it }, "ملاحظة (اختياري)")
                Spacer(Modifier.height(8.dp))
                if (txError.isNotBlank()) Text(txError, color = t.red)
                BigButton(if (editingTx == null) "حفظ العملية" else "حفظ تعديل العملية") {
                    val amount = txAmt.toDoubleOrNull()
                    val units = if (txUnits.isBlank()) 0.0 else txUnits.toDoubleOrNull()
                    if (amount == null || !amount.isFinite() || amount == 0.0 || units == null || !units.isFinite() || units < 0 || Finance.parseDate(txDate) == null) {
                        txError = "أدخل مبلغًا وتاريخًا وكمية صحيحة"
                        return@BigButton
                    }
                    val current = s.ins.find { it.id == ins.id } ?: return@BigButton
                    val proposed = current.copy(tx = current.tx.filter { it.id != editingTx }.toMutableList())
                    proposed.tx.add(com.mizanati.invest.data.Tx(id = editingTx ?: uid(), d = txDate, typ = txType, units = units, amt = amount))
                    var balance = 0.0
                    var invalidSale = false
                    proposed.tx.sortedBy { it.d }.forEach { transaction ->
                        if (transaction.typ == "buy") balance += transaction.units
                        if (transaction.typ == "sell" && transaction.units > 0) {
                            balance -= transaction.units
                            if (balance < -1e-9) invalidSale = true
                        }
                    }
                    if (invalidSale) {
                        txError = "كمية البيع أكبر من الرصيد المتاح في تاريخ العملية"
                        return@BigButton
                    }
                    val a = if (txType in listOf("sell", "coupon", "maturity", "rent")) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
                    onMutate { st3 ->
                        st3.ins.find { it.id == ins.id }?.let { tool ->
                            val old = tool.tx.find { it.id == editingTx }
                            val tx = com.mizanati.invest.data.Tx(
                                id = old?.id ?: uid(), d = txDate, amt = a, typ = txType, units = units, note = txNote,
                                fx = old?.fx ?: if (p.cur == "USD") s.fx.now else 0.0,
                            )
                            if (old == null) tool.tx.add(tx) else tool.tx[tool.tx.indexOf(old)] = tx
                        }
                    }
                    txAmt = ""; txUnits = ""; txNote = ""; editingTx = null; txError = ""
                }
                Spacer(Modifier.height(8.dp))
                BigButton("إغلاق", danger = true) { showTxFor = null }
            }
        }
    }
}
