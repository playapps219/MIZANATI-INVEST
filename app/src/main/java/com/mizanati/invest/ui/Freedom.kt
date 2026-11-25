package com.mizanati.invest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Calc
import com.mizanati.invest.core.D
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.Freedom
import com.mizanati.invest.core.f
import com.mizanati.invest.core.nz
import com.mizanati.invest.core.orElse
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.str
import org.json.JSONObject

private fun fmtUsd(v: Double): String = "$" + f(v, if (kotlin.math.abs(v) >= 1000) 0 else 2)
private fun fmtEgp(v: Double): String = f(v, 0) + " ج"
private fun fmt0(v: Double): String = f(v, 0)
private fun fmt1(v: Double): String = f(v, 1)
private fun fmtPct(v: Double): String = pc(v / 100.0)

private val ffTabs = listOf("overview" to "نظرة عامة", "items" to "الأصول والالتزامات", "safety" to "الأمان")

@Composable
fun FreedomScreen(app: AppState) {
    val engine = Engine(app.S)
    val f = Freedom(app.S, System.currentTimeMillis().toDouble())
    var tab by rememberSaveable { mutableStateOf("overview") }

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Seg(ffTabs, tab) { tab = it }
        when (tab) {
            "items" -> FreedomItems(app, f)
            "safety" -> FreedomSafety(app, engine, f)
            else -> FreedomOverview(app, engine, f)
        }
    }
}

@Composable
private fun FreedomOverview(app: AppState, engine: Engine, f: Freedom) {
    val mz = LocalMz.current
    val calc = engine.agg(engine.ins, false)
    val rate = f.blendRate(calc)
    val p0 = engine.totalPortfolioUSD
    val monthly = f.monthlyContrib()
    val y = f.yearsToFreedom(p0, rate, monthly, f.target(0.0))
    val netUsd = f.currentNetUsd(engine)
    val targetUsd = f.target(0.0)
    val prog = if (targetUsd > 0) netUsd / targetUsd else Double.NaN

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KGrid(
            listOf(
                KV("صافي الثروة الحالي", fmtUsd(netUsd)),
                KV("الهدف (x" + f.q.nz("factor").orElse(25.0).toInt() + " مصروف)", fmtUsd(targetUsd)),
                KV(
                    "السنوات للحرية",
                    if (y.isFinite() && y < 80) fmt1(y) + " سنة" else "غير محسوبة",
                    note = "عند استمرار المساهمات والعائد",
                ),
                KV("المصروف الشهري", fmtEgp(f.monthlySpendEgp()), note = "بسعر الصرف الحالي"),
            ),
        )
        if (prog.isFinite()) {
            Card {
                T("التقدم نحو الحرية", 14, FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Ring(prog, mz.p, 46.dp)
                    T(fmtPct(prog * 100.0), 15, FontWeight.Bold)
                }
            }
        }
        Card {
            T("مسار الثروة المتوقع", 14, FontWeight.Bold)
            T("بمعدل عائد مركب " + fmt1(rate * 100) + "% سنوياً ومساهمة " + fmtUsd(monthly) + "/شهر", 12, color = mz.m)
            listOf(0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0).forEach { yr ->
                val v = f.netAt(yr) + f.portfolioAt(engine, yr, p0, rate, monthly)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    T(if (yr == 0.0) "الآن" else "بعد " + yr.toInt() + " سنة", 13, color = mz.m)
                    T(fmtUsd(v), 13, FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("المصروف ومضاعف الهدف", Modifier.weight(1f)) { app.ffEditSpend = true }
        }
    }
    if (app.ffEditSpend) SpendDialog(app) { app.ffEditSpend = false }
}

@Composable
private fun SpendDialog(app: AppState, onDone: () -> Unit) {
    val mz = LocalMz.current
    val f = Freedom(app.S, System.currentTimeMillis().toDouble())
    var spend by remember { mutableStateOf(fmt0(f.q.nz("spend"))) }
    var factor by remember { mutableStateOf(fmt0(f.q.nz("factor").orElse(25.0))) }
    MzDialog("المصروف الشهري وهدف الحرية", onDone) {
        T("الهدف الافتراضي 25 ضعفاً من مصروفك الشهري (قاعدة 4%).", 12, color = mz.m)
        Field("المصروف الشهري (جنيه)", spend, { spend = it.filter { c -> c.isDigit() || c == '.' } }, keyboard = KeyboardType.Number, ltr = true)
        Field("مضاعف الهدف", factor, { factor = it.filter { c -> c.isDigit() || c == '.' } }, keyboard = KeyboardType.Number, ltr = true)
        Btn("حفظ", Modifier.fillMaxWidth()) {
            f.q.put("spend", D(spend)).put("factor", D(factor)).put("spendCur", "EGP")
            app.S.put("finFreedom", f.q)
            app.U()
            onDone()
        }
    }
}

private class ItemHolder(val o: JSONObject)

@Composable
private fun FreedomItems(app: AppState, f: Freedom) {
    val mz = LocalMz.current
    var edit by remember { mutableStateOf<ItemHolder?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            T("أصول والتزامات الحرية المالية", 14, FontWeight.Bold)
            T("الأصول والالتزامات خارج المحافظ تُحتسب في صافي ثروتك ومسار الحرية. أصول المحافظ تأتي تلقائياً من تبويب المحافظ.", 12, color = mz.m)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("+ أصل", Modifier.weight(1f)) { edit = ItemHolder(f.newItem("asset")) }
                Btn("+ التزام", Modifier.weight(1f), BtnKind.Ghost) { edit = ItemHolder(f.newItem("liab")) }
            }
        }
        val items = f.activeItems()
        if (items.isEmpty()) {
            Card { Note("لا أصول أو التزامات مسجلة بعد") }
        } else {
            items.sortedByDescending { f.toUsd(it.nz("v"), it.str("cur")) }.forEach { i ->
                val liab = i.str("kind") == "liab"
                Card(onClick = { edit = ItemHolder(i) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        T(i.str("name"), 14, FontWeight.Bold)
                        T(
                            (if (liab) "− " else "") + fmtUsd(f.toUsd(i.nz("v"), i.str("cur"))),
                            14,
                            FontWeight.Bold,
                            if (liab) mz.r else mz.p,
                        )
                    }
                    val bits = mutableListOf<String>()
                    if (i.nz("g") != 0.0) bits.add("نمو " + fmt1(i.nz("g")) + "%/سنة")
                    if (i.nz("mi") != 0.0) bits.add("إضافة " + fmt0(i.nz("mi")) + " " + (if (i.str("cur") == "USD") "$" else "ج") + "/شهر")
                    if (bits.isNotEmpty()) T(bits.joinToString(" · "), 11, color = mz.m)
                }
            }
        }
    }
    edit?.let { h -> FreedomItemDialog(app, f, h.o) { edit = null } }
}

@Composable
private fun FreedomItemDialog(app: AppState, f: Freedom, item: JSONObject, onDone: () -> Unit) {
    val mz = LocalMz.current
    var name by remember { mutableStateOf(item.str("name")) }
    var v by remember { mutableStateOf(fmt0(item.nz("v"))) }
    var g by remember { mutableStateOf(fmt1(item.nz("g"))) }
    var mi by remember { mutableStateOf(fmt0(item.nz("mi"))) }
    var cur by remember { mutableStateOf(item.str("cur").ifEmpty { "EGP" }) }
    var kind by remember { mutableStateOf(item.str("kind").ifEmpty { "asset" }) }
    val isNew = f.item(item.str("id")) == null

    MzDialog(if (isNew) "بند جديد" else "تعديل البند", onDone) {
        Seg(listOf("أصل" to "asset", "التزام" to "liab"), kind) { kind = it }
        Seg(listOf("جنيه" to "EGP", "دولار" to "USD"), cur) { cur = it }
        Field("الاسم", name, { name = it })
        Field("القيمة الحالية", v, { v = it.filter { c -> c.isDigit() || c == '.' || c == '-' } }, keyboard = KeyboardType.Number, ltr = true)
        Field("النمو السنوي المتوقع %", g, { g = it.filter { c -> c.isDigit() || c == '.' || c == '-' } }, keyboard = KeyboardType.Number, ltr = true)
        Field("إضافة شهرية", mi, { mi = it.filter { c -> c.isDigit() || c == '.' || c == '-' } }, keyboard = KeyboardType.Number, ltr = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("حفظ", Modifier.weight(1f)) {
                item.put("name", name.ifBlank { if (kind == "liab") "التزام" else "أصل" })
                    .put("kind", kind)
                    .put("cur", cur)
                    .put("v", D(v))
                    .put("g", D(g))
                    .put("mi", D(mi))
                val items = f.items().toMutableList()
                val idx = items.indexOfFirst { it.str("id") == item.str("id") }
                if (idx >= 0) items[idx] = item else items.add(item)
                f.saveItems(items)
                app.U()
                onDone()
            }
            if (!isNew) Btn("حذف", Modifier.weight(1f), BtnKind.Danger) {
                f.removeItem(item.str("id"))
                app.U()
                onDone()
            }
        }
    }
}

@Composable
private fun FreedomSafety(app: AppState, engine: Engine, f: Freedom) {
    val mz = LocalMz.current
    val q = f.q
    var months by remember { mutableStateOf(fmt0(q.nz("safetyMonths").orElse(6.0))) }

    val balUsd = engine.emergencyLedgerBalance(engine.emergencyTx) ?: 0.0
    val efTargetUsd = f.toUsd(f.monthlySpendEgp() * D(months).orElse(6.0), "EGP")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            T("شبكة الأمان", 14, FontWeight.Bold)
            T("كم شهراً من مصروفك يغطيه صندوق الطوارئ؟", 12, color = mz.m)
            Field("أشهر الأمان المستهدفة", months, { months = it.filter { c -> c.isDigit() || c == '.' } }, keyboard = KeyboardType.Number, ltr = true)
            Btn("حفظ", Modifier.fillMaxWidth()) {
                q.put("safetyMonths", D(months))
                app.S.put("finFreedom", q)
                app.U()
            }
        }
        KGrid(
            listOf(
                KV("رصيد صندوق الطوارئ", fmtUsd(balUsd)),
                KV("الهدف (" + fmt0(D(months).orElse(6.0)) + " شهر)", fmtUsd(efTargetUsd)),
            ),
        )
        val ratio = if (efTargetUsd > 0) balUsd / efTargetUsd else Double.NaN
        if (ratio.isFinite()) {
            Card {
                T("تغطية صندوق الطوارئ", 14, FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Ring(ratio, if (ratio >= 1) mz.p else mz.p, 46.dp)
                    T(fmtPct(ratio * 100.0), 15, FontWeight.Bold)
                }
                T(
                    if (ratio >= 1) "صندوق الطوارئ مكتمل" else "تبقى " + fmtUsd((efTargetUsd - balUsd).coerceAtLeast(0.0)),
                    12,
                    color = mz.m,
                )
            }
        }
    }
}
