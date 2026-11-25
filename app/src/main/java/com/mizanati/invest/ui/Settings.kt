package com.mizanati.invest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.PAL
import com.mizanati.invest.core.backupPayload
import com.mizanati.invest.core.f
import com.mizanati.invest.core.fxStats
import com.mizanati.invest.core.jobj
import com.mizanati.invest.core.jsonArrayOf
import com.mizanati.invest.core.list
import com.mizanati.invest.core.mkP
import com.mizanati.invest.core.nz
import com.mizanati.invest.core.pIcon
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.seed
import com.mizanati.invest.core.set
import com.mizanati.invest.core.str
import com.mizanati.invest.core.truthy
import com.mizanati.invest.core.uid
import org.json.JSONObject

private val PF_ICONS = listOf("💵", "💰", "🥇", "🏢", "📈", "🏦", "🏠", "🪙", "💎", "🛡️")

@Composable
fun SettingsScreen(app: AppState) {
    val version = app.version
    val S = app.S
    val e = remember(version) { Engine(S) }
    var investorName by remember(version) { mutableStateOf(S.optString("investorName")) }
    var newPfName by remember { mutableStateOf("") }
    var newPfCur by remember { mutableStateOf("EGP") }
    var importText by remember { mutableStateOf("") }
    var showExport by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val wallets = S.list("pfs").filter { !it.truthy("isEmergency") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            T("اسم المستثمر", 15, FontWeight.Bold)
            Field("اسم المستثمر", investorName, { investorName = it }, placeholder = "مثال: محمد نجاح")
            Btn("حفظ الاسم") {
                S.put("investorName", investorName.trim())
                app.toast = if (investorName.trim().isEmpty()) "تم مسح اسم المستثمر" else "تم حفظ اسم المستثمر"
                app.U()
            }
        }

        Card {
            T("نسبة الدولار والجنيه", 15, FontWeight.Bold)
            val split = S.nz("split").let { if (it == 0.0) 80.0 else it }
            NumField("الدولار %", f(split, 0), {
                val v = it.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: return@NumField
                S.put("split", v)
                app.U()
            })
            T(f(100 - split, 0) + "% جنيه · " + f(split, 0) + "% دولار", 12, color = LocalMz.current.m)
        }

        Card {
            T("سعر الصرف وانخفاض الجنيه", 15, FontWeight.Bold)
            val fx = e.S.optJSONObject("fx") ?: JSONObject()
            var book by remember(version) { mutableStateOf(if (fx.nz("book") == 0.0) "" else f(fx.nz("book"), 2)) }
            var now by remember(version) { mutableStateOf(if (fx.nz("now") == 0.0) "" else f(fx.nz("now"), 2)) }
            NumField("السعر الدفتري (ج)", book, {
                book = it
                it.toDoubleOrNull()?.let { v -> fx.set("book", v); app.U() }
            })
            DateField("تاريخ السعر الدفتري", fx.str("bookDate"), { fx.put("bookDate", it); app.U() })
            NumField("السعر الحالي (ج)", now, {
                now = it
                it.toDoubleOrNull()?.let { v -> fx.set("now", v); fx.put("upd", "يدوي"); app.U() }
            })
            val st = e.fxStats()
            KGrid(
                listOf(
                    KV("ارتفاع الدولار", if (st.ok) pc(st.usdUp) else "—"),
                    KV("انخفاض قيمة الجنيه", if (st.ok) pc(st.egpDown) else "—"),
                    KV("المعدل السنوي", if (st.ann.isFinite()) pc(st.ann) else "—"),
                    KV("آخر تحديث", fx.str("upd").ifEmpty { "—" }),
                ),
            )
            Note(if (!st.ok) "أدخل السعر الدفتري والحالي وتاريخاً سابقاً لليوم." else "المدة " + f(st.days, 0) + " يوم · المعدل = 1 − (الدفتري ÷ الحالي)^(365 ÷ المدة).")
        }

        wallets.forEach { p ->
            PortfolioSettings(app, p)
        }

        Card {
            T("محفظة جديدة", 15, FontWeight.Bold)
            Field("اسم المحفظة", newPfName, { newPfName = it })
            Seg(listOf("EGP" to "جنيه", "USD" to "دولار"), newPfCur) { newPfCur = it }
            Btn("＋ إضافة المحفظة", enabled = newPfName.isNotBlank()) {
                val pf = mkP(newPfName.trim(), newPfCur, listOf("دخل ثابت", "أسهم"), PAL[S.list("pfs").size % PAL.size], 12.0)
                S.optJSONArray("pfs")?.put(pf)
                newPfName = ""
                app.toast = "تمت إضافة المحفظة"
                app.U()
            }
        }

        Card {
            T("النسخ الاحتياطي والبيانات", 15, FontWeight.Bold)
            Btn("⬇ تصدير JSON") { showExport = true }
            Field("استيراد JSON (الصق النص هنا)", importText, { importText = it })
            Btn("↥ استيراد JSON", kind = BtnKind.Ghost, enabled = importText.isNotBlank()) {
                val data = try {
                    JSONObject(importText)
                } catch (_: Exception) {
                    null
                }
                if (data == null) {
                    app.toast = "نص JSON غير صالح"
                } else if (app.replace(data)) {
                    importText = ""
                    app.toast = "تم استيراد البيانات"
                } else {
                    app.toast = "تعذر استيراد البيانات"
                }
            }
            Btn("↩ استرجاع الافتراضي", kind = BtnKind.Danger) { confirmReset = true }
        }

        LockSettingsCard(app)
    }

    if (showExport) {
        MzDialog("تصدير JSON", { showExport = false }, full = true) {
            T("انسخ هذا النص واحفظه في مكان آمن:", 12, color = LocalMz.current.m)
            T(backupPayload(S), 11)
        }
    }
    if (confirmReset) {
        ConfirmDialog("مسح كل البيانات نهائياً والاسترجاع الافتراضي؟", "استرجاع", { confirmReset = false }) {
            confirmReset = false
            app.replace(seed())
            app.toast = "تم الاسترجاع الافتراضي"
        }
    }
}

@Composable
private fun PortfolioSettings(app: AppState, p: JSONObject) {
    val version = app.version
    var name by remember(version) { mutableStateOf(p.str("name")) }
    var tRet by remember(version) { mutableStateOf(f(p.nz("tRet"), 1)) }
    var secNames by remember(version) {
        mutableStateOf(p.list("secs").joinToString("، ") { it.str("name") })
    }
    var newSec by remember { mutableStateOf("") }

    Card {
        T("محفظة: " + p.str("name"), 15, FontWeight.Bold, color = hex(p.str("col")))
        Field("اسم المحفظة", name, {
            name = it
            p.put("name", it)
            app.U()
        })
        Seg(PF_ICONS.map { it to it }, pIcon(p).let { if (PF_ICONS.contains(it)) it else PF_ICONS.first() }) {
            p.put("icon", it)
            app.U()
        }
        NumField("العائد السنوي المستهدف %", tRet, {
            tRet = it
            it.toDoubleOrNull()?.let { v -> p.set("tRet", v); app.U() }
        })
        Seg(listOf(true to "ظاهرة في الرئيسية", false to "مخفية"), p.truthy("showHome") || !p.has("showHome")) {
            p.put("showHome", it)
            app.U()
        }
        T("الأقسام", 13, FontWeight.Bold)
        Field("الأقسام مفصولة بفاصلة", secNames, { secNames = it })
        Btn("حفظ الأقسام", kind = BtnKind.Ghost) {
            val secs = com.mizanati.invest.core.jsonArrayOf(
                secNames.split("،", ",").mapNotNull { s ->
                    val n = s.trim()
                    if (n.isEmpty()) null else jobj("id" to uid(), "name" to n)
                },
            )
            p.put("secs", secs)
            app.U()
        }
        Btn("🗑 حذف المحفظة", kind = BtnKind.Danger) {
            app.S.put("pfs", com.mizanati.invest.core.jsonArrayOf(app.S.list("pfs").filter { it.str("id") != p.str("id") }))
            app.curP = "all"
            app.toast = "تم حذف المحفظة"
            app.U()
        }
    }
}
