package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.mizanati.invest.core.HomeSummary
import com.mizanati.invest.core.PfRow
import com.mizanati.invest.core.f
import com.mizanati.invest.core.homeSummary
import com.mizanati.invest.core.jsTruthy
import com.mizanati.invest.core.num
import com.mizanati.invest.core.pIcon
import com.mizanati.invest.core.pc
import com.mizanati.invest.core.profitRatio
import com.mizanati.invest.core.str
import com.mizanati.invest.core.tf
import kotlinx.coroutines.delay
import org.json.JSONObject

private class Prompt(val text: String, val by: String)

private val PROMPTS = listOf(
    Prompt("استثمر بانتظام وفق خطتك، ولا تنتظر لحظة مثالية لاختيار توقيت السوق.", "تذكير بالاستثمار الدوري · صياغة إرشادية"),
    Prompt("امنح استثماراتك وقتًا، وراقب تكاليفها؛ فالمدى الطويل مهم.", "مستوحاة من مبدأ جاك بوغل"),
    Prompt("فكّر في الاستثمار كملكية طويلة الأجل، لا كصفقة قصيرة الأجل.", "مستوحاة من رسالة وارن بافيت للمساهمين، 1996"),
    Prompt("الصبر جزء من الخطة؛ لا تدع تقلبات اليوم تقود قرارك.", "مستوحاة من مبادئ تشارلي مانغر في الصبر"),
    Prompt("لا تطارد تحركات السوق؛ اختر خطة تناسب أهدافك وقدرتك على تحمل المخاطر.", "مستوحاة من مبادئ جاك بوغل الاستثمارية"),
    Prompt("الاستمرار على خطة مدروسة أهدأ من محاولة التنبؤ بكل صعود وهبوط.", "مستوحاة من جاك بوغل · بصياغة حرة"),
)

private class AnTile(val key: String, val title: String, val c1: Color, val c2: Color, val icon: String)

private val AN_TILES = listOf(
    AnTile("xirr", "تفصيل العائد XIRR", Color(0xFF6366F1), Color(0xFF8B5CF6), "↗"),
    AnTile("pf", "توزيع المحافظ", Color(0xFF06B6D4), Color(0xFF3B82F6), "◔"),
    AnTile("cur", "توزيع العملات", Color(0xFF10B981), Color(0xFF14B8A6), "⇄"),
    AnTile("data", "بيانات الأدوات", Color(0xFFF59E0B), Color(0xFFEF4444), "▤"),
    AnTile("focus", "بوصلة التركيز والأهداف", Color(0xFF4338CA), Color(0xFF0891B2), "◎"),
)

private val FALLBACK_COLORS = listOf("#635bff", "#19b8a8", "#ff9f43", "#9b5de5")

private fun isEmergency(p: JSONObject) = jsTruthy(p.opt("isEmergency"))

private fun money(sign: String, v: Double): String = (if (v < 0) "\u200E−" else "") + sign + f(kotlin.math.abs(v))

@Composable
fun HomeScreen(app: AppState, onOpenPortfolio: (String) -> Unit) {
    var analysisKey by remember { mutableStateOf<String?>(null) }
    val version = app.version
    val e = remember(version) { Engine(app.S) }
    val hs = remember(version) { e.homeSummary() }
    var showList by remember { mutableStateOf(false) }
    var showAnalysis by remember { mutableStateOf(false) }
    val walletRows = hs.rows.filter { !isEmergency(it.p) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeHero(e, hs)
        InvestorBand()
        Destination("🏦", "المحافظ الاستثمارية", "${walletRows.size} محافظ · اضغط لفتح القائمة كاملة") { showList = true }
        Destination("✦", "مركز التحليل", "المؤشرات الاستثمارية وأدوات التخطيط المالي") { showAnalysis = true }
        if (app.S.optJSONArray("pfs")?.length() == 0) Card { T("لا توجد محافظ. أضف من الإعدادات.", 13) }
    }

    if (showList) {
        MzDialog("المحافظ الاستثمارية", { showList = false }, full = true) {
            PortfolioList(e, hs) { id ->
                showList = false
                onOpenPortfolio(id)
            }
        }
    }
    if (showAnalysis) {
        MzDialog("مركز التحليل", { showAnalysis = false }, full = true) {
            AnalysisCenter(e, hs, walletRows) { key ->
                showAnalysis = false
                analysisKey = key
            }
        }
    }
    analysisKey?.let { k -> AnalysisDialog(app, k) { analysisKey = null } }
}

@Composable
private fun HomeHero(e: Engine, hs: HomeSummary) {
    val fn = e.fn
    Hero(Color(0xFF4F46E5), "لوحة تحكم جميع المحافظ", "$" + f(hs.tot), "≈ ج" + f(hs.tot * fn) + " بسعر " + f(fn, 2)) {
        KGrid(
            listOf(
                KV("إجمالي الربح (محقق + متوقع)", "$" + f(hs.profit, 0), cl(hs.profit)),
                KV("نسبة الربح", pc(hs.ratio), cl(hs.ratio)),
                KV("XIRR الإجمالي (بسعر العملية المسجّل)", pc(hs.xU.f), cl(hs.xU.f)),
                KV("أثر الصرف على محافظ الدولار (ج)", money("ج", hs.fxg), cl(hs.fxg)),
                KV("أثر الصرف على محافظ الجنيه ($)", money("$", hs.fxu), cl(hs.fxu)),
                KV("العمر المرجح", f(hs.age) + " يوم"),
            ),
        )
        KBox(
            KV(
                "💱 صافي ربح الصرف (مكسب الدولاري − خسارة الجنيهي)",
                money("$", hs.fxt),
                cl(hs.fxt),
                "≈ ج" + f(kotlin.math.abs(hs.fxt) * fn),
            ),
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InvestorBand() {
    val mz = LocalMz.current
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            index = (index + 1) % PROMPTS.size
        }
    }
    val item = PROMPTS[index]
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(mz.p.copy(alpha = .10f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(mz.p),
            contentAlignment = Alignment.Center,
        ) { T("↗", 16, FontWeight.Bold, Color.White) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            T("✦ استثمر بوعي", 11, FontWeight.ExtraBold, mz.p)
            T(item.text, 13, FontWeight.Bold)
            T(item.by, 10, color = mz.m)
        }
    }
}

@Composable
private fun Destination(icon: String, title: String, sub: String, onClick: () -> Unit) {
    val mz = LocalMz.current
    Card(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(mz.c2),
                contentAlignment = Alignment.Center,
            ) { T(icon, 20) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(title, 15, FontWeight.ExtraBold)
                T(sub, 11, color = mz.m)
            }
            T("←", 16, FontWeight.Bold, mz.m)
        }
    }
}

@Composable
private fun PortfolioList(e: Engine, hs: HomeSummary, onOpen: (String) -> Unit) {
    val mz = LocalMz.current
    val rows = hs.rows.filter { !isEmergency(it.p) }
    T("القيمة والربح وXIRR والاحتياج الاستثماري لكل محفظة", 11, color = mz.m)
    rows.forEachIndexed { idx, x -> PortfolioCard(e, x, hs.tot, idx, onOpen) }
    EmergencyCard(e)
    if (rows.isEmpty() && !jsTruthy(e.ef.opt("showHome"))) {
        T("لا توجد محافظ ظاهرة. يمكنك تفعيلها من الإعدادات.", 13, color = mz.m)
    }
}

@Composable
private fun PortfolioCard(e: Engine, x: PfRow, total: Double, idx: Int, onOpen: (String) -> Unit) {
    val mz = LocalMz.current
    val p = x.p
    val a = x.A
    val eg = p.str("cur") == "EGP"
    val c = e.sym(p)
    val col = hex(p.str("col").ifEmpty { FALLBACK_COLORS[idx % FALLBACK_COLORS.size] })
    val share = if (total != 0.0) x.usd / total else 0.0
    val rr = if (a.cost != 0.0) (a.real + a.unr) / a.cost else 0.0
    val target = p.num("tRet") / 100
    val tgt = e.pfTgt(p)
    val progress = if (tgt > 0) a.value / tgt else 0.0
    val targetIns = x.L.filter { it.str("tgtMode") == "pct" || it.num("tgt") > 0 }
    var needText = "—"
    var needNote: String? = null
    var needColor: Color? = null
    if (targetIns.isNotEmpty()) {
        val gap = targetIns.fold(0.0) { s, i -> s + e.insTgt(i) - e.fin(e.calc(i), eg).value }
        needText = c + f(kotlin.math.abs(gap))
        needColor = if (gap > .005) mz.a else if (gap < -.005) mz.r else null
        needNote = if (gap > .005) "مطلوب للاستثمار" else if (gap < -.005) "فائض استثمار" else "الأهداف مكتملة"
    } else {
        needNote = "حدّد أهداف الأدوات"
    }
    Card(onClick = { onOpen(p.str("id")) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(col.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) { T(pIcon(p), 20) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(p.str("name"), 14, FontWeight.ExtraBold, maxLines = 1)
                T("${p.str("cur")} · ${x.L.size} أداة · ${f(a.age)} يوم · ${tf(share * 100, 0)}% من الإجمالي", 10, color = mz.m, maxLines = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                T(c + f(a.value), 14, FontWeight.ExtraBold, maxLines = 1)
                T(pc(rr), 11, FontWeight.Bold, cl(rr))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("XIRR " + (if (eg) "(بالجنيه) " else "(بالدولار) ") + pc(a.xr), 11, FontWeight.Bold, cl(a.xr))
            T("الهدف " + tf(p.num("tRet"), 2) + "%", 11, color = mz.m)
        }
        MiniBull(a.xr, target, Modifier.fillMaxWidth())
        KGrid(
            listOf(
                KV("الربح (محقق + متوقع)", c + f(a.real + a.unr), cl(a.real + a.unr)),
                KV(if (targetIns.isNotEmpty()) "صافي احتياج الاستثمار" else "المطلوب للاستثمار", needText, needColor, needNote),
            ),
        )
        if (tgt > 0) {
            Bar(progress.coerceIn(0.0, 1.0), col, Modifier.fillMaxWidth(), 6.dp)
            T(pc(progress) + " من هدف " + c + f(tgt), 10, color = mz.m)
        }
        KGrid(
            listOf(
                KV(if (eg) "XIRR (بالدولار مع الصرف)" else "XIRR (بالجنيه مع الصرف)", pc(a.xu)),
                KV("نسبة الربح", pc(profitRatio(a)), cl(profitRatio(a))),
                KV("نسبة الربح المحقق", pc(if (a.buys != 0.0) a.real / a.buys else Double.NaN)),
                KV("التكلفة", c + f(a.cost)),
            ),
        )
    }
}

@Composable
private fun EmergencyCard(e: Engine) {
    val ef = e.ef
    if (!jsTruthy(ef.opt("showHome"))) return
    val mz = LocalMz.current
    val usd = ef.str("currency") == "USD"
    val cur = if (usd) "$" else "ج"
    val value = e.emergencyMarketValue()
    val invested = jsTruthy(ef.opt("invested"))
    val months = kotlin.math.max(.5, ef.num("safetyMonths").let { if (it.isFinite() && it != 0.0) it else 3.0 })
    val threshold = months * kotlin.math.max(0.0, ef.num("monthlySpend").let { if (it.isFinite()) it else 0.0 })
    val cover = if (threshold > 0) value / threshold else Double.NaN
    val need = kotlin.math.max(0.0, threshold - value)
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Green.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) { T("🛟", 20) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T("صندوق الطوارئ", 14, FontWeight.ExtraBold, maxLines = 1)
                T((if (invested) "مستثمر" else "نقدي") + " · " + (if (usd) "دولار أمريكي" else "جنيه مصري"), 10, color = mz.m)
            }
            T(cur + f(value), 14, FontWeight.ExtraBold, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("تغطية حد الأمان " + pc(cover), 11, FontWeight.Bold, cl(if (cover.isFinite()) cover - 1 else 0.0))
            T(if (threshold > 0) "المستهدف " + cur + f(threshold) else "حد الأمان غير محدد", 11, color = mz.m)
        }
        Bar(if (cover.isFinite()) cover.coerceIn(0.0, 1.0) else 0.0, if (cover >= 1) Green else Amber, Modifier.fillMaxWidth(), 6.dp)
        KBox(
            KV(
                "احتياج الإيداع لبلوغ حد الأمان",
                if (threshold > 0) cur + f(need) else "حدّد حد الأمان",
                if (threshold > 0) (if (need > 0) mz.r else mz.a) else null,
            ),
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnalysisCenter(e: Engine, hs: HomeSummary, walletRows: List<PfRow>, onOpen: (String) -> Unit) {
    val mz = LocalMz.current
    val values = mapOf(
        "xirr" to pc(hs.xU.f) + " $ · " + pc(hs.xE.f) + " ج",
        "pf" to "${walletRows.size} محافظ",
        "cur" to pc(hs.usdPct) + " $ · " + pc(hs.egpPct) + " ج",
        "data" to e.ins.count { i -> walletRows.any { it.p.str("id") == i.str("pf") } }.toString() + " أداة",
        "focus" to tf(e.pctAllocated(), 2) + "% مستخدمة",
    )
    T("اختر أيقونة لفتح لوحة بيانات مستقلة وتفاعلية", 11, color = mz.m)
    AN_TILES.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { t -> AnalysisTile(t, values[t.key] ?: "—", Modifier.weight(1f)) { onOpen(t.key) } }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(2.dp))
    Destination("↗", "نمو العوائد عبر الزمن", "تحليل تفاعلي للمحافظ والأدوات الاستثمارية بالعملتين") { onOpen("growth") }
}

@Composable
private fun AnalysisTile(t: AnTile, value: String, modifier: Modifier, onClick: () -> Unit) {
    val mz = LocalMz.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(mz.c2)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cssGradient(135f, 0f to t.c1, 1f to t.c2)),
            contentAlignment = Alignment.Center,
        ) { T(t.icon, 18, FontWeight.Bold, Color.White) }
        T(t.title, 12, FontWeight.ExtraBold)
        T(value, 11, color = mz.m, maxLines = 2)
    }
}
