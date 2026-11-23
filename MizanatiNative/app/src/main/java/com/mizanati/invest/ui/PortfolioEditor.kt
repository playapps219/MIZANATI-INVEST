package com.mizanati.invest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mizanati.invest.data.*

@Composable
fun PortfolioEditor(p: Portfolio, onMutate: ((AppState) -> Unit) -> Unit) {
    var open by remember(p.id) { mutableStateOf(false) }
    var name by remember(p.id, p.name) { mutableStateOf(p.name) }
    var target by remember(p.id, p.tAmt) { mutableStateOf(p.tAmt.toString()) }
    var rate by remember(p.id, p.tRet) { mutableStateOf(p.tRet.toString()) }
    var sectionName by remember(p.id) { mutableStateOf("") }
    var error by remember(p.id) { mutableStateOf("") }
    Card2 {
        BigButton(if (open) "إغلاق إدارة المحفظة والأقسام" else "إدارة المحفظة والأقسام") { open = !open }
        if (open) {
            Spacer(Modifier.height(10.dp))
            Field(name, { name = it }, "اسم المحفظة")
            Field(target, { target = it }, "القيمة المستهدفة (${p.cur})")
            Field(rate, { rate = it }, "العائد السنوي المستهدف %")
            Spacer(Modifier.height(8.dp))
            BigButton("حفظ بيانات المحفظة") {
                val amount = target.toDoubleOrNull()
                val annual = rate.toDoubleOrNull()
                if (name.isBlank() || amount == null || !amount.isFinite() || amount < 0 || annual == null || !annual.isFinite()) {
                    error = "أدخل اسمًا وقيمًا رقمية صحيحة"
                } else {
                    onMutate { state -> state.pfs.find { it.id == p.id }?.let {
                        it.name = name.trim(); it.tAmt = amount; it.tRet = annual
                    } }
                    error = ""
                }
            }
            Spacer(Modifier.height(10.dp))
            SectionTitle("الأقسام الاستثمارية")
            p.secs.forEach { section ->
                SectionEditor(p.id, section, onMutate)
            }
            Field(sectionName, { sectionName = it }, "اسم القسم الجديد")
            BigButton("إضافة قسم") {
                if (sectionName.isBlank()) error = "أدخل اسم القسم"
                else if (p.secs.any { it.name == sectionName.trim() }) error = "هذا القسم موجود بالفعل"
                else {
                    onMutate { state -> state.pfs.find { it.id == p.id }?.secs?.add(Sec(uid(), sectionName.trim())) }
                    sectionName = ""; error = ""
                }
            }
            if (error.isNotBlank()) Text(error, color = LocalTheme.red)
        }
    }
}

@Composable
private fun SectionEditor(pf: String, section: Sec, onMutate: ((AppState) -> Unit) -> Unit) {
    var name by remember(section.id, section.name) { mutableStateOf(section.name) }
    var deleting by remember(section.id) { mutableStateOf(false) }
    Field(name, { name = it }, "اسم القسم")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("حفظ الاسم", false) {
            if (name.isNotBlank()) onMutate { state ->
                state.pfs.find { it.id == pf }?.secs?.find { it.id == section.id }?.name = name.trim()
            }
        }
        Chip("حذف القسم", deleting) { deleting = !deleting }
    }
    if (deleting) {
        Muted("سيُحذف القسم فقط؛ ستظل أدواته محفوظة ضمن جميع الأدوات.")
        BigButton("تأكيد حذف القسم", danger = true) {
            onMutate { state ->
                state.pfs.find { it.id == pf }?.secs?.removeAll { it.id == section.id }
                state.ins.filter { it.pf == pf && it.sec == section.id }.forEach { it.sec = "" }
            }
            deleting = false
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun InstrumentEditor(i: Instrument, p: Portfolio, onMutate: ((AppState) -> Unit) -> Unit, onClose: () -> Unit) {
    var name by remember(i.id) { mutableStateOf(i.name) }
    var section by remember(i.id) { mutableStateOf(i.sec) }
    var kind by remember(i.id) { mutableStateOf(i.kind) }
    var price by remember(i.id) { mutableStateOf(i.price.toString()) }
    var mode by remember(i.id) { mutableStateOf(i.mode) }
    var target by remember(i.id) { mutableStateOf(i.tgt.toString()) }
    var expected by remember(i.id) { mutableStateOf(i.ret.toString()) }
    var principal by remember(i.id) { mutableStateOf(i.price0.toString()) }
    var rate by remember(i.id) { mutableStateOf(i.rate.toString()) }
    var term by remember(i.id) { mutableStateOf(i.termM.toString()) }
    var frequency by remember(i.id) { mutableStateOf(i.pmtM.toString()) }
    var start by remember(i.id) { mutableStateOf(i.start.ifBlank { today() }) }
    var rent by remember(i.id) { mutableStateOf(i.rent.toString()) }
    var expenses by remember(i.id) { mutableStateOf(i.expM.toString()) }
    var rentStart by remember(i.id) { mutableStateOf(i.rentStart.ifBlank { today() }) }
    var fee by remember(i.id) { mutableStateOf(i.feePct.toString()) }
    var error by remember(i.id) { mutableStateOf("") }
    Card2 {
        SectionTitle("تعديل الأداة الاستثمارية")
        Field(name, { name = it }, "اسم الأداة")
        listOf("fund" to "صندوق", "gold" to "ذهب", "cert" to "شهادة", "stock" to "أسهم", "cash" to "نقد", "prop" to "عقار").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (id, label) -> Chip(label, kind == id) { kind = id } }
            }
        }
        Spacer(Modifier.height(8.dp))
        Chip("دون قسم", section.isBlank()) { section = "" }
        p.secs.forEach { sec -> Chip(sec.name, section == sec.id) { section = sec.id } }
        Field(target, { target = it }, "القيمة المستهدفة")
        Field(expected, { expected = it }, "العائد السنوي المتوقع %")
        if (kind in listOf("fund", "gold", "stock")) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("قيمة إجمالية", mode == "v") { mode = "v" }
                Chip("سعر الوحدة", mode == "p") { mode = "p" }
            }
            Field(price, { price = it }, if (kind == "gold") "سعر الجرام" else "سعر الوثيقة / السهم")
        }
        if (kind in listOf("cert", "prop")) {
            Field(principal, { principal = it }, "مبلغ الشراء")
            Field(start, { start = it }, "تاريخ الشراء (yyyy-MM-dd)")
            Field(fee, { fee = it }, "المصاريف %")
        }
        if (kind == "cert") {
            Field(rate, { rate = it }, "العائد السنوي %")
            Field(term, { term = it }, "المدة بالشهور")
            Field(frequency, { frequency = it }, "دورية العائد بالشهور")
        }
        if (kind == "prop") {
            Field(rent, { rent = it }, "الإيجار الشهري")
            Field(expenses, { expenses = it }, "المصاريف الشهرية")
            Field(rentStart, { rentStart = it }, "بداية الإيجار (yyyy-MM-dd)")
        }
        if (error.isNotBlank()) Text(error, color = LocalTheme.red)
        BigButton("حفظ الأداة") {
            val numeric = listOf(price, target, expected, principal, rate, rent, expenses, fee).map { it.toDoubleOrNull() }
            if (name.isBlank() || numeric.any { it == null || !it.isFinite() || it < 0 } ||
                (kind in listOf("cert", "prop") && com.mizanati.invest.calc.Finance.parseDate(start) == null) ||
                (kind == "prop" && com.mizanati.invest.calc.Finance.parseDate(rentStart) == null) ||
                (kind == "cert" && ((term.toIntOrNull() ?: 0) <= 0 || (frequency.toIntOrNull() ?: 0) <= 0))) {
                error = "راجع الاسم والأرقام والتواريخ؛ المدة ودورية العائد يجب أن تكونا أكبر من صفر"
            } else {
                onMutate { state -> state.ins.find { it.id == i.id }?.let { tool ->
                    tool.name = name.trim(); tool.sec = section; tool.kind = kind
                    tool.price = price.toDouble(); tool.mode = mode; tool.tgt = target.toDouble()
                    tool.ret = expected.toDouble(); tool.price0 = principal.toDouble()
                    tool.rate = rate.toDouble(); tool.termM = term.toIntOrNull() ?: 0
                    tool.pmtM = frequency.toIntOrNull() ?: 0; tool.start = start
                    tool.rent = rent.toDouble(); tool.expM = expenses.toDouble()
                    tool.rentStart = rentStart; tool.feePct = fee.toDouble(); tool.buyD = start
                } }
                onClose()
            }
        }
        Spacer(Modifier.height(8.dp))
        BigButton("إلغاء", danger = true, onClick = onClose)
    }
}
