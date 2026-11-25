package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.arr
import com.mizanati.invest.core.f
import com.mizanati.invest.core.insIcon
import com.mizanati.invest.core.jobj
import com.mizanati.invest.core.jsonArrayOf
import com.mizanati.invest.core.kindOf
import com.mizanati.invest.core.latinDigits
import com.mizanati.invest.core.list
import com.mizanati.invest.core.num
import com.mizanati.invest.core.pIcon
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.profitRatio
import com.mizanati.invest.core.secIcon
import com.mizanati.invest.core.secRows
import com.mizanati.invest.core.str
import com.mizanati.invest.core.tf
import com.mizanati.invest.core.today
import com.mizanati.invest.core.uid
import org.json.JSONObject

val SecPal = listOf(
    Color(0xFF635BFF), Color(0xFF19B8A8), Color(0xFFFF9F43), Color(0xFF9B5DE5), Color(0xFF3B82F6),
    Color(0xFFEF4444), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF06B6D4), Color(0xFFEC4899),
)

fun numStr(v: Double): String =
    if (v.isNaN() || v == 0.0) "" else java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

fun parseNum(s: String): Double = latinDigits(s).trim().replace(",", "").replace("٫", ".").toDoubleOrNull() ?: 0.0

private sealed class PDlg {
    object AddSec : PDlg()
    class RenSec(val sid: String, val name: String) : PDlg()
    class DelSec(val sid: String, val n: Int) : PDlg()
}

@Composable
fun PortfolioScreen(app: AppState) {
    val version = app.version
    val e = remember(version) { Engine(app.S) }
    val p = e.P(app.curP)
    val mz = LocalMz.current
    val E = p.str("cur") == "EGP"
    val L = e.insOf(p)
    val A = e.agg(L, E)
    val c = e.sym(p)
    val col = hex(p.str("col"))
    val rows = e.secRows(p, A)
    var dlg by remember { mutableStateOf<PDlg?>(null) }
    val open = remember { mutableStateMapOf<String, Boolean>() }
    val names = remember { mutableStateMapOf<String, String>() }
    val tRet = p.num("tRet")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Btn("→ كل المحافظ", kind = BtnKind.Ghost) { app.curP = "all" }
        val msg = if (A.xr.isFinite()) {
            if (A.xr * 100 >= tRet) "أعلى من المطلوب" else "أقل من المطلوب بـ " + tf(tRet - A.xr * 100, 1) + "%"
        } else {
            "أدخل عمليات وقيمة لحساب XIRR"
        }
        Hero(col, p.str("name") + " · " + p.str("cur"), c + f(A.value), "الهدف السنوي " + tf(tRet, 2) + "% · " + msg) { KG(e, A, p) }

        if (rows.isNotEmpty()) {
            Card {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    T("توزيع أقسام المحفظة", 14, FontWeight.ExtraBold)
                    T(c + f(A.value), 12, color = mz.m)
                }
                val sorted = rows.sortedByDescending { it.R.value }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Donut(sorted.mapIndexed { k, r -> r.R.value to SecPal[k % SecPal.size] }, 120.dp, 16.dp) {
                        T(c + f(A.value), 11, FontWeight.Bold, maxLines = 1)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sorted.forEachIndexed { k, r -> Legend(SecPal[k % SecPal.size], r.s.str("name"), pc(r.pct)) }
                    }
                }
            }
        }

        rows.forEachIndexed { idx, r ->
            val sid = r.s.str("id")
            val title = secIcon(r.s.str("name")) + " " + r.s.str("name") + " · " + r.L.size + " أداة · " + c + f(r.R.value)
            Collapsible(title, open[sid] == true, { open[sid] = open[sid] != true }) {
                T("إجمالي الربح " + c + f(r.R.tot) + " · XIRR " + pc(r.R.xr) + " · نسبة الربح " + pc(profitRatio(r.R)), 11, color = cl(r.R.tot))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Btn("↑", kind = BtnKind.Ghost) { moveSec(app, p, idx, -1) }
                    Btn("↓", kind = BtnKind.Ghost) { moveSec(app, p, idx, 1) }
                    Btn("تعديل الاسم", Modifier.weight(1f), BtnKind.Ghost) { dlg = PDlg.RenSec(sid, r.s.str("name")) }
                    Btn("حذف القسم", Modifier.weight(1f), BtnKind.Danger) { dlg = PDlg.DelSec(sid, r.L.size) }
                }
                r.L.forEachIndexed { k, i -> InstrumentRow(e, i, p, r.R.value, k) { app.openIns = i.str("id") } }
                T("أداة استثمارية جديدة", 11, FontWeight.Bold, mz.m)
                Field("اسم الأداة", names[sid] ?: "", { names[sid] = it })
                Btn("إضافة", Modifier.fillMaxWidth()) {
                    val n = (names[sid] ?: "").trim()
                    if (n.isEmpty()) {
                        app.toast = "⚠️ أدخل اسم الأداة الاستثمارية أولاً"
                    } else {
                        val id = newInstrument(app, p, r.s, n)
                        names[sid] = ""
                        open[sid] = true
                        app.openIns = id
                    }
                }
            }
        }
        Btn("＋ إضافة قسم جديد", Modifier.fillMaxWidth(), BtnKind.Ghost) { dlg = PDlg.AddSec }
    }

    when (val d = dlg) {
        is PDlg.AddSec -> NameDialog("قسم جديد", "اسم القسم", "", "إضافة", { dlg = null }) { n ->
            p.arr("secs").put(jobj("id" to uid(), "name" to n))
            app.toast = "تمت إضافة القسم"
            app.U()
        }
        is PDlg.RenSec -> NameDialog("تعديل اسم القسم", "اسم القسم", d.name, "حفظ", { dlg = null }) { n ->
            p.list("secs").firstOrNull { it.str("id") == d.sid }?.put("name", n)
            app.toast = "تم التعديل"
            app.U()
        }
        is PDlg.DelSec -> ConfirmDialog("حذف القسم" + (if (d.n > 0) " و" + d.n + " أداة بداخله" else "") + "؟", "حذف", { dlg = null }) {
            val keep = p.list("secs").filter { it.str("id") != d.sid }
            p.put("secs", jsonArrayOf(keep))
            val ins = app.S.list("ins").filter { it.str("sec") != d.sid }
            app.S.put("ins", jsonArrayOf(ins))
            app.U()
        }
        null -> {}
    }

    val oi = app.openIns
    if (oi != null && app.S.list("ins").any { it.str("id") == oi }) InstrumentDialog(app, oi)
}

private fun moveSec(app: AppState, p: JSONObject, i: Int, d: Int) {
    val a = p.list("secs").toMutableList()
    val j = i + d
    if (j < 0 || j >= a.size) return
    val t = a[i]
    a[i] = a[j]
    a[j] = t
    p.put("secs", jsonArrayOf(a))
    app.U()
}

fun newInstrument(app: AppState, p: JSONObject, s: JSONObject, name: String): String {
    val kind = kindOf(p, s)
    val now = app.S.optJSONObject("fx")?.num("now") ?: 0.0
    val o = jobj(
        "id" to uid(), "pf" to p.str("id"), "sec" to s.str("id"), "name" to name, "kind" to kind,
        "tx" to org.json.JSONArray(), "price" to 0, "total" to 0, "mode" to "p", "cashVal" to 0,
    )
    if (kind == "cert") {
        o.put("amt", 0); o.put("rate", 20); o.put("freq", 1); o.put("start", today())
        o.put("term", 12); o.put("feePct", 0); o.put("feeFix", 0); o.put("fx", now)
    }
    if (kind == "prop") {
        o.put("amt", 0); o.put("start", today()); o.put("buyCosts", 0); o.put("fx", now); o.put("rent", 0)
        o.put("rentStart", today()); o.put("expM", 0); o.put("expY", 0); o.put("curVal", 0)
    }
    app.S.arr("ins").put(o)
    app.U()
    return o.str("id")
}

@Composable
fun NameDialog(title: String, label: String, initial: String, confirm: String, onClose: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    var err by remember { mutableStateOf(false) }
    MzDialog(title, onClose) {
        Field(label, text, { text = it; err = false })
        if (err) Note("⚠️ الاسم فارغ", LocalMz.current.r)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(confirm, Modifier.weight(1f)) {
                val n = text.trim()
                if (n.isEmpty()) {
                    err = true
                } else {
                    onSave(n)
                    onClose()
                }
            }
            Btn("إلغاء", Modifier.weight(1f), BtnKind.Ghost, onClick = onClose)
        }
    }
}

@Composable
fun InstrumentRow(e: Engine, i: JSONObject, p: JSONObject, secVal: Double, k: Int, onClick: () -> Unit) {
    val mz = LocalMz.current
    val c = e.fin(e.calc(i), p.str("cur") == "EGP")
    val cu = e.sym(p)
    val kind = i.str("kind")
    val sub = when (kind) {
        "prop" -> cu + f(i.num("amt")) + " · إيجار " + cu + f(i.num("rent")) + "/شهر"
        "cert" -> f(i.num("amt")) + " " + cu + " · " + i.str("rate") + "% · " + (if (c.matured) "مستحقة" else "سارية")
        "gold" -> f(c.u, 3) + " جرام · " + cu + f(c.tot)
        else -> if (c.u > 0) f(c.u, 4) + " وثيقة · تكلفة " + cu + f(c.avg, 4) else i.list("tx").size.toString() + " عملية · " + cu + f(c.value)
    }
    val target = p.num("tRet") / 100
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(mz.c2)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Ring(if (secVal > 0) c.value / secVal else 0.0, SecPal[k % SecPal.size], 42.dp) { T(insIcon(i), 14) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(i.str("name"), 13, FontWeight.ExtraBold, maxLines = 1)
                T(sub, 10, color = mz.m, maxLines = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                T(cu + f(c.tot), 13, FontWeight.ExtraBold, cl(c.tot), maxLines = 1)
                T(pc(profitRatio(c)) + " · XIRR " + pc(c.xr), 10, color = cl(profitRatio(c)), maxLines = 1)
            }
        }
        MiniBull(c.xr, target, Modifier.fillMaxWidth())
    }
}
