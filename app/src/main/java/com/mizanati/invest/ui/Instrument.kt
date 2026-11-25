package com.mizanati.invest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Calc
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.arr
import com.mizanati.invest.core.dm
import com.mizanati.invest.core.f
import com.mizanati.invest.core.fd
import com.mizanati.invest.core.insIcon
import com.mizanati.invest.core.jobj
import com.mizanati.invest.core.jsonArrayOf
import com.mizanati.invest.core.list
import com.mizanati.invest.core.num
import com.mizanati.invest.core.nz
import com.mizanati.invest.core.pIcon
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.pd
import com.mizanati.invest.core.profitRatio
import com.mizanati.invest.core.str
import com.mizanati.invest.core.tf
import com.mizanati.invest.core.today
import com.mizanati.invest.core.uid
import org.json.JSONObject

private class TxForm(
    val id: String?,
    var kind: String,
    var date: String,
    var type: String,
    var amt: String,
    var units: String,
    var fx: String,
)

@Composable
fun InstrumentDialog(app: AppState, id: String) {
    val version = app.version
    val i = app.S.list("ins").firstOrNull { it.str("id") == id } ?: return
    val e = remember(version) { Engine(app.S) }
    val p = e.P(i.str("pf"))
    val mz = LocalMz.current
    val E = p.str("cur") == "EGP"
    val c = e.fin(e.calc(i), E)
    val cu = e.sym(p)
    val kind = i.str("kind")
    val gold = kind == "gold"
    val secName = p.list("secs").firstOrNull { it.str("id") == i.str("sec") }?.str("name") ?: ""
    var tx by remember { mutableStateOf<TxForm?>(null) }
    var delIns by remember { mutableStateOf(false) }
    var delTx by remember { mutableStateOf<String?>(null) }
    val target = p.num("tRet") / 100
    val rr = profitRatio(c)
    val rc = if (c.cost > 0) c.real / c.cost else 0.0
    val uc = if (c.cost > 0) c.unr / c.cost else 0.0

    MzDialog(insIcon(i) + " " + i.str("name"), { app.openIns = null }, full = true) {
        T(pIcon(p) + " " + p.str("name") + " · " + secName, 11, color = mz.m)
        if (kind == "fund" || gold) {
            Card {
                T("📦 ما تملكه الآن", 13, FontWeight.ExtraBold)
                val items = ArrayList<KV>()
                if (c.u > 0) items.add(KV(if (gold) "عدد الجرامات المملوكة" else "عدد الوثائق المملوكة", f(c.u, if (gold) 3 else 4)))
                items.add(KV("المبلغ المستثمر", cu + f(c.cost, 2)))
                if (c.u > 0) items.add(KV(if (gold) "متوسط سعر الجرام" else "متوسط سعر التكلفة", cu + f(c.avg, if (gold) 2 else 4)))
                KGrid(items)
            }
        }
        ChBox("XIRR مقابل هدف المحفظة", Modifier.fillMaxWidth()) {
            Gauge(c.xr, target)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T((if (E) "بالجنيه " else "بالدولار ") + pc(c.xr), 11, FontWeight.Bold, cl(c.xr))
                T((if (E) "بالدولار " else "بالجنيه ") + pc(c.xu), 11, FontWeight.Bold, cl(c.xu))
            }
        }
        ChBox("الربح والقيمة", Modifier.fillMaxWidth()) {
            T("نسبة الربح " + pc(rr) + " · " + cu + f(c.tot), 13, FontWeight.ExtraBold, cl(rr))
            SplitBar(rc, uc)
            ProfitBars(listOf(PB("المستثمر", c.cost, mz.m), PB("القيمة", c.value, hex(p.str("col")))), cu)
        }
        if (c.cf.any { it.v != 0.0 && !it.v.isNaN() }) {
            ChBox("مسار رأس المال المستثمر عبر الزمن", Modifier.fillMaxWidth()) { PathChart(c, cu) }
        }

        when (kind) {
            "cert" -> CertSection(app, i, p, c, cu)
            "prop" -> PropSection(app, i, c, cu)
            else -> FundValueSection(app, i, c, cu, gold)
        }
        KG(e, c, p, true)

        Btn("＋ تسجيل عملية جديدة", Modifier.fillMaxWidth()) {
            tx = TxForm(null, if (gold) "units" else "cash", today(), "buy", "", "", numStr(e.fn))
        }
        T("سجل العمليات (" + i.list("tx").size + ")", 13, FontWeight.ExtraBold)
        if (i.list("tx").isEmpty()) Note("لا عمليات بعد.")
        i.list("tx").sortedByDescending { it.str("date") }.forEach { t ->
            Card {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        T((if (t.str("type") == "buy") "شراء" else "استرداد") + " · " + dm(t.str("date")), 12, FontWeight.Bold)
                        val det = if (t.str("kind") == "units") {
                            f(t.num("units"), 4) + (if (gold) " جرام × " else " وثيقة × ") + f(t.num("amt") / t.num("units"), 4)
                        } else {
                            "مبلغ مالي"
                        }
                        T(det + " · دولار " + f(t.num("fx"), 2), 10, color = mz.m)
                    }
                    T(cu + f(t.num("amt"), 2), 13, FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("✎ تعديل", Modifier.weight(1f), BtnKind.Ghost) {
                        tx = TxForm(
                            t.str("id"), t.str("kind"), t.str("date"), t.str("type"),
                            numStr(t.num("amt")), numStr(t.num("units")), numStr(t.num("fx")),
                        )
                    }
                    Btn("✕ حذف", Modifier.weight(1f), BtnKind.Danger) { delTx = t.str("id") }
                }
            }
        }
        Btn("حذف الأداة ✕", Modifier.fillMaxWidth(), BtnKind.Danger) { delIns = true }
    }

    tx?.let { form ->
        TxDialog(app, i, e, gold, cu, form) { tx = null }
    }
    if (delIns) {
        ConfirmDialog("حذف الأداة وكل عملياتها؟", "حذف", { delIns = false }) {
            app.S.put("ins", jsonArrayOf(app.S.list("ins").filter { it.str("id") != id }))
            app.openIns = null
            app.toast = "تم الحذف"
            app.U()
        }
    }
    delTx?.let { tid ->
        ConfirmDialog("حذف هذه العملية؟ لا يمكن التراجع.", "حذف", { delTx = null }) {
            val old = i.list("tx")
            i.put("tx", jsonArrayOf(old.filter { it.str("id") != tid }))
            if (Engine(app.S).calc(i).err) {
                i.put("tx", jsonArrayOf(old))
                app.toast = "⚠️ الحذف يجعل عمليات لاحقة غير صالحة"
            } else {
                app.toast = "تم الحذف"
                app.U()
            }
        }
    }
}

@Composable
private fun FundValueSection(app: AppState, i: JSONObject, c: Calc, cu: String, gold: Boolean) {
    val units = gold || i.list("tx").any { it.str("kind") == "units" }
    val cash = !gold && i.list("tx").any { it.str("kind") == "cash" }
    val both = !gold && !units && !cash
    val byTotal = i.str("mode") == "t"
    var price by remember(app.version) {
        mutableStateOf(if (byTotal && c.u > 0) numStr(c.value / c.u) else numStr(i.num("price")))
    }
    var total by remember(app.version) { mutableStateOf(if (byTotal) numStr(i.num("total")) else "") }
    var cv by remember(app.version) { mutableStateOf(if (i.num("cashVal") > 0) numStr(i.num("cashVal")) else "") }
    if (units || both) {
        Card {
            T(if (gold) "💎 سعر الجرام الحالي" else "💎 القيمة الحالية للوثائق", 13, FontWeight.ExtraBold)
            NumField(if (gold) "سعر الجرام (ج)" else "سعر الوثيقة", price, { price = it }, Modifier.fillMaxWidth())
            Btn("⟳ تحديث السعر", Modifier.fillMaxWidth()) {
                val v = parseNum(price)
                if (!(v > 0)) {
                    app.toast = "⚠️ أدخل سعراً صحيحاً أكبر من صفر"
                } else {
                    i.put("mode", "p")
                    i.put("price", v)
                    app.toast = "✅ تم تحديث البيانات"
                    app.U()
                }
            }
            NumField(if (gold) "أو إجمالي قيمة الذهب" else "أو إجمالي قيمة الوثائق", total, { total = it }, Modifier.fillMaxWidth())
            Btn("⟳ تحديث الإجمالي", Modifier.fillMaxWidth(), BtnKind.Ghost) {
                val v = parseNum(total)
                if (!(v > 0)) {
                    app.toast = "⚠️ أدخل قيمة صحيحة أكبر من صفر"
                } else {
                    i.put("mode", "t")
                    i.put("total", v)
                    app.toast = "✅ تم تحديث البيانات"
                    app.U()
                }
            }
        }
    }
    if (cash || both) {
        Card {
            T("💎 قيمة المحفظة الحالية (مبالغ مالية)", 13, FontWeight.ExtraBold)
            NumField("القيمة الآن (" + cu + ")", cv, { cv = it }, Modifier.fillMaxWidth(), f(c.cost, 2))
            Note("حدّثها عند الحاجة فقط؛ الإيداعات والاستردادات الجديدة تُضاف تلقائياً بعد تاريخ التحديث.")
            Btn("⟳ تحديث", Modifier.fillMaxWidth()) {
                val v = parseNum(cv)
                if (!(v > 0)) {
                    app.toast = "⚠️ أدخل قيمة صحيحة أكبر من صفر"
                } else {
                    i.put("cashVal", v)
                    i.put("cvDate", today())
                    app.toast = "✅ تم تحديث البيانات"
                    app.U()
                }
            }
        }
    }
}

private class FieldDef(val label: String, val key: String, val date: Boolean = false)

@Composable
private fun FieldsCard(app: AppState, i: JSONObject, title: String, defs: List<FieldDef>, required: String, extra: @Composable () -> Unit = {}) {
    val vals = remember(app.version) {
        defs.associate { d -> d.key to mutableStateOf(if (d.date) i.str(d.key).ifEmpty { today() } else numStr(i.num(d.key))) }
    }
    Card {
        T(title, 13, FontWeight.ExtraBold)
        defs.forEach { d ->
            val st = vals.getValue(d.key)
            if (d.date) DateField(d.label, st.value, { st.value = it }, Modifier.fillMaxWidth())
            else NumField(d.label, st.value, { st.value = it }, Modifier.fillMaxWidth())
        }
        extra()
        Btn("حفظ", Modifier.fillMaxWidth()) {
            defs.forEach { d ->
                val s = vals.getValue(d.key).value
                if (d.date) i.put(d.key, pd(s).ifEmpty { today() }) else i.put(d.key, parseNum(s))
            }
            if (!(i.num("amt") > 0)) {
                app.toast = required
            } else {
                if (i.num("fx") <= 0) i.put("fx", Engine(app.S).fn)
                if (i.str("kind") == "cert" && i.num("term") <= 0) i.put("term", 12)
                app.toast = "تم الحفظ"
                app.U()
            }
        }
    }
}

@Composable
private fun CertSection(app: AppState, i: JSONObject, p: JSONObject, c: Calc, cu: String) {
    val x = c.x
    KGrid(
        listOf(
            KV("كوبون دوري (إجمالي)", cu + f(x["per"] ?: Double.NaN, 2)),
            KV("كوبون دوري (صافي)", cu + f(x["netP"] ?: Double.NaN, 2)),
            KV("العائد السنوي الصافي", pc(x["yn"] ?: Double.NaN)),
            KV("الصرف", f(x["rc"] ?: 0.0) + " من " + f(x["n"] ?: 0.0)),
            KV("الاستحقاق", fd(x["matT"] ?: Double.NaN)),
            KV("XIRR لكامل المدة", pc(x["xf"] ?: Double.NaN)),
        ),
    )
    var freq by remember(app.version) { mutableStateOf(i.num("freq").let { if (it.isNaN()) 1.0 else it }) }
    FieldsCard(
        app, i, "بيانات الشهادة",
        listOf(
            FieldDef("مبلغ الشراء (" + cu + ")", "amt"), FieldDef("نسبة العائد السنوي %", "rate"),
            FieldDef("تاريخ الشراء", "start", true), FieldDef("المدة (شهر)", "term"),
            FieldDef("مصاريف/ضريبة % من العائد", "feePct"), FieldDef("مصاريف ثابتة لكل صرف", "feeFix"),
            FieldDef("سعر الدولار وقت الشراء", "fx"),
        ),
        "⚠️ أدخل مبلغ الشراء",
    ) {
        T("دورية الصرف", 11, color = LocalMz.current.m)
        Seg(listOf(1.0 to "شهري", 3.0 to "ربع", 6.0 to "نصف", 12.0 to "سنوي", 0.0 to "نهاية"), freq) {
            freq = it
            i.put("freq", it)
            app.U()
        }
    }
}

@Composable
private fun PropSection(app: AppState, i: JSONObject, c: Calc, cu: String) {
    val x = c.x
    KGrid(
        listOf(
            KV("إجمالي الإيجار المحصَّل", cu + f(x["inc"] ?: 0.0)),
            KV("إجمالي المصروفات", cu + f(x["exp"] ?: 0.0)),
            KV("صافي الدخل المحقق", cu + f(x["net"] ?: 0.0), cl(x["net"] ?: 0.0)),
            KV("مكسب تغير قيمة العقار", cu + f(x["cg"] ?: 0.0), cl(x["cg"] ?: 0.0)),
            KV("العائد الإيجاري الإجمالي", pc(x["gy"] ?: Double.NaN)),
            KV("العائد الإيجاري الصافي", pc(x["ny"] ?: Double.NaN)),
        ),
    )
    FieldsCard(
        app, i, "بيانات العقار",
        listOf(
            FieldDef("سعر العقار (" + cu + ")", "amt"), FieldDef("مصاريف الشراء والتسجيل", "buyCosts"),
            FieldDef("تاريخ الشراء", "start", true), FieldDef("سعر الدولار وقت الشراء", "fx"),
            FieldDef("الإيجار الشهري", "rent"), FieldDef("تاريخ بدء الإيجار", "rentStart", true),
            FieldDef("مصروفات شهرية", "expM"), FieldDef("مصروفات سنوية", "expY"),
            FieldDef("القيمة السوقية الحالية (فارغة = التكلفة)", "curVal"),
        ),
        "⚠️ أدخل سعر العقار",
    )
    Note("الربح = صافي الدخل (الإيجار − المصروفات) + (القيمة الحالية − السعر ومصاريف الشراء).")
}

@Composable
private fun TxDialog(app: AppState, i: JSONObject, e: Engine, gold: Boolean, cu: String, form: TxForm, onClose: () -> Unit) {
    var kind by remember { mutableStateOf(if (gold) "units" else form.kind) }
    var date by remember { mutableStateOf(form.date) }
    var type by remember { mutableStateOf(form.type) }
    var amt by remember { mutableStateOf(form.amt) }
    var units by remember { mutableStateOf(form.units) }
    var fx by remember { mutableStateOf(form.fx) }
    var err by remember { mutableStateOf<String?>(null) }
    MzDialog(if (form.id != null) "تعديل العملية" else "إضافة عملية جديدة", onClose) {
        if (!gold) Seg(listOf("cash" to "مبلغ مالي", "units" to "وثائق استثمار"), kind) { kind = it }
        DateField("التاريخ", date, { date = it }, Modifier.fillMaxWidth())
        Seg(listOf("buy" to "شراء", "sell" to "استرداد"), type) { type = it }
        NumField("المبلغ (" + cu + ")", amt, { amt = it }, Modifier.fillMaxWidth())
        if (kind == "units") NumField(if (gold) "عدد الجرامات" else "عدد الوثائق", units, { units = it }, Modifier.fillMaxWidth())
        NumField("سعر الدولار", fx, { fx = it }, Modifier.fillMaxWidth())
        err?.let { Note(it, LocalMz.current.r) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("حفظ", Modifier.weight(1f)) {
                val a = parseNum(amt)
                val u = parseNum(units)
                val rate = parseNum(fx).let { if (it > 0) it else e.fn }
                if (!(a > 0)) {
                    err = "⚠️ أدخل مبلغاً صحيحاً"
                } else if (date.isEmpty()) {
                    err = "⚠️ اختر التاريخ"
                } else if (kind == "units" && !(u > 0)) {
                    err = "⚠️ أدخل عدد الوثائق"
                } else {
                    val t = jobj(
                        "id" to (form.id ?: uid()), "kind" to kind, "date" to date, "type" to type,
                        "amt" to a, "units" to (if (kind == "units") u else 0.0), "fx" to rate,
                    )
                    val old = i.list("tx")
                    val next = if (form.id != null) old.map { if (it.str("id") == form.id) t else it } else old + t
                    i.put("tx", jsonArrayOf(next))
                    if (Engine(app.S).calc(i).err) {
                        i.put("tx", jsonArrayOf(old))
                        err = "⚠️ وثائق الاسترداد أكبر من المتاح في هذا التاريخ"
                    } else {
                        app.toast = if (form.id != null) "تم تعديل العملية" else "تم حفظ العملية"
                        app.U()
                        onClose()
                    }
                }
            }
            Btn("إلغاء", Modifier.weight(1f), BtnKind.Ghost, onClick = onClose)
        }
    }
}
