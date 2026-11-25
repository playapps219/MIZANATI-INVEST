package com.mizanati.invest.core

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

val PAL = listOf("#635bff", "#06b6d4", "#f59e0b", "#10b981", "#ef4444", "#8b5cf6", "#ec4899", "#84cc16", "#0ea5e9", "#f97316")

val KIND_NOTES = mapOf(
    "cert" to "الأدوات في هذا القسم تُنشأ كشهادات بنكية.",
    "gold" to "ذهب عيني: عدد الجرامات ومبلغ الشراء، ويُحسب متوسط سعر الجرام والمكسب.",
    "prop" to "عقار عيني: السعر والإيجار الشهري والمصروفات والقيمة الحالية.",
)

fun kindOf(p: JSONObject, s: JSONObject): String {
    val n = s.str("name")
    if (n.contains("شهاد")) return "cert"
    if (n.contains("عيني")) {
        if (n.contains("ذهب") || p.str("id") == "gold") return "gold"
        if (n.contains("عقار") || p.str("id") == "re") return "prop"
    }
    return "fund"
}

fun pIcon(p: JSONObject): String {
    val icon = p.str("icon")
    if (icon.isNotEmpty()) return icon
    val name = p.str("name")
    return when {
        name.contains("ذهب") || p.str("id") == "gold" -> "🥇"
        name.contains("عقار") || p.str("id") == "re" -> "🏢"
        name.contains("أسهم") -> "📈"
        p.str("cur") == "USD" -> "💵"
        else -> "🇪🇬"
    }
}

fun secIcon(n: String): String = when {
    n.contains("شهاد") -> "🏦"
    n.contains("ذهب") -> "🥇"
    n.contains("عقار") -> "🏢"
    n.contains("أسهم") -> "📈"
    n.contains("دخل") || n.contains("سندات") -> "🛡️"
    n.contains("صناديق") -> "🧺"
    else -> "📁"
}

fun insIcon(i: JSONObject): String = when (i.str("kind")) {
    "gold" -> "🥇"
    "prop" -> "🏠"
    "cert" -> "🏦"
    else -> "📈"
}

class GrowthParts(
    val events: List<Flow>,
    val unrealized: Double,
    val basis: Double,
    val fallback: Int,
    val dailyProxy: Int,
)

fun Engine.growthParts(i: JSONObject, p: JSONObject, c: Calc, mode: String, outCur: String, avgFx: Double, nowFx: Double): GrowthParts {
    val native = p.str("cur")
    val ev = ArrayList<Flow>()
    var fallback = 0
    var dailyProxy = 0
    var unr: Double
    val fxRecorded = i.num("fx") > 0
    val fx0 = if (fxRecorded) i.num("fx") else nowFx
    fun otherCost(amt: Double, fx: Double) = if (native == "EGP") amt / fx else amt * fx
    fun rateFor(recorded: Double) = when (mode) {
        "historical" -> if (recorded > 0) recorded else nowFx
        "average" -> avgFx
        else -> nowFx
    }
    fun add(t: Double, v: Double, recorded: Double) {
        if (v.isFinite() && abs(v) > 1e-10) ev.add(Flow(t, gConv(v, native, outCur, rateFor(recorded))))
    }
    val reportedLike = mode == "historical" || mode == "reported"
    val kind = i.str("kind")
    if (kind == "prop") {
        val rent = i.nz("rent")
        val eM = i.nz("expM")
        val eY = i.nz("expY")
        val start = i.str("start")
        val rentStart = i.str("rentStart").ifEmpty { start }
        for (k in 1..1200) {
            val t = addM(rentStart, k)
            if (t > now) break
            if (rent != 0.0) add(t, rent, fx0)
        }
        for (k in 1..1200) {
            val t = addM(start, k)
            if (t > now) break
            if (eM != 0.0) add(t, -eM, fx0)
        }
        for (k in 1..100) {
            val t = addM(start, 12 * k)
            if (t > now) break
            if (eY != 0.0) add(t, -eY, fx0)
        }
        unr = when {
            outCur == native -> c.unr
            reportedLike -> gConv(c.value, native, outCur, nowFx) - gConv(c.cost, native, outCur, fx0)
            else -> gConv(c.unr, native, outCur, rateFor(fx0))
        }
        if (mode == "historical") {
            dailyProxy++
            if (!fxRecorded) fallback++
        } else if (mode == "average" && !fxRecorded) fallback++
    } else if (kind == "cert") {
        val A = i.nz("amt")
        val m = max(1.0, i.nz("freq").orElse(i.nz("term")).orElse(1.0))
        val term = max(1.0, i.nz("term").orElse(1.0))
        val gross = A * i.nz("rate") / 100 * m / 12
        val coupon = gross - gross * i.nz("feePct") / 100 - i.nz("feeFix")
        val n = max(1.0, floor(term / m + 1e-9)).toInt()
        val start = i.str("start")
        for (k in 1..n) {
            val t = addM(start, (k * m).toInt())
            if (t > now) break
            if (coupon != 0.0) add(t, coupon, fx0)
        }
        val matT = addM(start, i.nz("term").toInt())
        val matured = matT <= now
        if (matured && native != outCur) {
            val exitRate = if (mode == "average") avgFx else nowFx
            val fxGain = gConv(A, native, outCur, exitRate) - gConv(A, native, outCur, fx0)
            if (abs(fxGain) > 1e-10) ev.add(Flow(matT, fxGain))
            if (mode == "historical") {
                dailyProxy++
                if (!fxRecorded) fallback++
            } else if (mode == "average" && !fxRecorded) fallback++
        }
        unr = when {
            matured -> 0.0
            outCur == native -> c.unr
            reportedLike -> gConv(c.value, native, outCur, nowFx) - gConv(c.cost, native, outCur, fx0)
            else -> gConv(c.unr, native, outCur, rateFor(fx0))
        }
        if (mode == "historical") {
            dailyProxy++
            if (!fxRecorded) fallback++
        } else if (mode == "average" && !fxRecorded) fallback++
    } else {
        var units = 0.0
        var uCost = 0.0
        var uOther = 0.0
        var cashCost = 0.0
        var cashOther = 0.0
        val cashVal = i.num("cashVal")
        for (t in i.list("tx").sortedBy { it.str("date") }) {
            val buy = t.str("type") == "buy"
            val amt = t.nz("amt")
            val hasFx = t.num("fx") > 0
            val txFx = if (hasFx) t.num("fx") else nowFx
            val date = D(t.str("date"))
            if (mode != "current" && !hasFx) fallback++
            val isUnits = t.str("kind") == "units"
            if (buy) {
                val oc = otherCost(amt, txFx)
                if (isUnits) {
                    units += t.nz("units")
                    uCost += amt
                    uOther += oc
                } else {
                    cashCost += amt
                    cashOther += oc
                }
                continue
            }
            val localProfit: Double
            var otherProfit = 0.0
            if (isUnits) {
                val q = t.nz("units")
                if (!(units > 0) || q > units + 1e-9) continue
                val fr = min(1.0, q / units)
                val releasedNative = uCost * fr
                val releasedOther = uOther * fr
                localProfit = amt - releasedNative
                if (outCur != native) otherProfit = gConv(amt, native, outCur, txFx) - releasedOther
                units -= q
                uCost -= releasedNative
                uOther -= releasedOther
                if (units < 1e-9) {
                    units = 0.0
                    uCost = 0.0
                    uOther = 0.0
                }
            } else {
                val base = if (cashVal > 0) cashVal + amt else cashCost
                val fr = if (base > 0) min(1.0, amt / base) else 0.0
                val releasedNative = cashCost * fr
                val releasedOther = cashOther * fr
                localProfit = amt - releasedNative
                if (outCur != native) otherProfit = gConv(amt, native, outCur, txFx) - releasedOther
                cashCost -= releasedNative
                cashOther -= releasedOther
                if (cashCost < 1e-6) {
                    cashCost = 0.0
                    cashOther = 0.0
                }
            }
            ev.add(
                Flow(
                    date,
                    when {
                        outCur == native -> localProfit
                        reportedLike -> otherProfit
                        else -> gConv(localProfit, native, outCur, rateFor(txFx))
                    },
                ),
            )
        }
        unr = when {
            outCur == native -> c.unr
            reportedLike -> gConv(c.value, native, outCur, nowFx) - (uOther + cashOther)
            else -> gConv(c.unr, native, outCur, rateFor(fx0))
        }
    }
    val basis = when {
        outCur == native -> c.cost
        reportedLike -> c.costU
        else -> gConv(c.cost, native, outCur, if (mode == "average") avgFx else nowFx)
    }
    return GrowthParts(ev, if (unr.isFinite()) unr else 0.0, if (basis.isFinite()) basis else 0.0, fallback, dailyProxy)
}

class PfRow(val p: JSONObject, val A: Calc, val usd: Double, val L: List<JSONObject>)

class XTrip(val a: Double, val f: Double, val d: Double)

class HomeSummary(
    val rows: List<PfRow>,
    val tot: Double,
    val profit: Double,
    val ratio: Double,
    val fxt: Double,
    val fxu: Double,
    val fxg: Double,
    val age: Double,
    val xU: XTrip,
    val xE: XTrip,
    val usdVal: Double,
    val egpVal: Double,
    val usdPct: Double,
    val egpPct: Double,
)

fun Engine.homeSummary(): HomeSummary {
    val fn = fn
    var tot = 0.0
    var pt = 0.0
    var recordedCostUsd = 0.0
    var fxt = 0.0
    var fxu = 0.0
    var fxg = 0.0
    var ageNum = 0.0
    var ageDen = 0.0
    val rows = pfs.filter { it.opt("showHome") != false }.map { p ->
        val L = insOf(p)
        val E = p.str("cur") == "EGP"
        val A = agg(L, E)
        val usd = if (E) A.value / fn else A.value
        val avgFx = growthAvgFx(L, fn)
        for (i in L) {
            val g = growthParts(i, p, fin(calc(i), E), "historical", "USD", avgFx, fn)
            pt += g.events.fold(0.0) { a, e -> a + e.v } + g.unrealized
            recordedCostUsd += g.basis
        }
        tot += usd
        fxt += if (E) A.fxe else A.fxe / fn
        if (E) fxu += A.fxe else fxg += A.fxe
        val ageCost = if (E) A.cost / fn else A.cost
        ageNum += A.age * ageCost
        ageDen += ageCost
        PfRow(p, A, usd, L)
    }
    fun isE(x: PfRow) = x.p.str("cur") == "EGP"
    fun trip(a: Double, f: Double) = XTrip(a, f, if (a.isFinite() && f.isFinite()) f - a else Double.NaN)
    val termF = Flow(now, tot)
    val xAll = xirr(rows.flatMap { x -> if (isE(x)) x.A.cf.map { Flow(it.t, it.v / fn) } else x.A.cf } + termF)
    val xAllFx = xirr(rows.flatMap { x -> if (isE(x)) x.A.cfU else x.A.cf } + termF)
    val termE = Flow(now, tot * fn)
    val xAllE = xirr(rows.flatMap { x -> if (isE(x)) x.A.cf else x.A.cf.map { Flow(it.t, it.v * fn) } } + termE)
    val xAllFxE = xirr(rows.flatMap { x -> if (isE(x)) x.A.cf else x.A.cfU } + termE)
    val usdVal = rows.filter { !isE(it) }.fold(0.0) { a, x -> a + x.A.value }
    val egpVal = rows.filter { isE(it) }.fold(0.0) { a, x -> a + x.A.value / fn }
    val usdPct = usdVal / (usdVal + egpVal).orElse(1.0)
    return HomeSummary(
        rows = rows,
        tot = tot,
        profit = pt,
        ratio = if (recordedCostUsd > 0) pt / recordedCostUsd else Double.NaN,
        fxt = fxt,
        fxu = fxu,
        fxg = fxg,
        age = if (ageDen != 0.0) ageNum / ageDen else 0.0,
        xU = trip(xAll, xAllFx),
        xE = trip(xAllE, xAllFxE),
        usdVal = usdVal,
        egpVal = egpVal,
        usdPct = usdPct,
        egpPct = 1 - usdPct,
    )
}

class SecRow(val s: JSONObject, val L: List<JSONObject>, val R: Calc, val pct: Double, val index: Int)

fun Engine.secRows(p: JSONObject, A: Calc): List<SecRow> {
    val E = p.str("cur") == "EGP"
    return p.list("secs").mapIndexed { idx, s ->
        val L = ins.filter { it.str("sec") == s.str("id") }
        val R = agg(L, E)
        SecRow(s, L, R, if (A.value != 0.0) R.value / A.value else 0.0, idx)
    }
}

class FxStats(val ok: Boolean, val days: Double, val usdUp: Double, val egpDown: Double, val ann: Double)

fun Engine.fxStats(): FxStats {
    val fx = S.optJSONObject("fx") ?: JSONObject()
    val b = fx.num("book")
    val n = fx.num("now")
    val days = (now - D(fx.str("bookDate"))) / 864e5
    val ok = b > 0 && n > 0 && days.isFinite() && days >= 1
    return FxStats(
        ok,
        days,
        if (ok) n / b - 1 else Double.NaN,
        if (ok) 1 - b / n else Double.NaN,
        if (ok && days >= 30) 1 - Math.pow(b / n, 365.25 / days) else Double.NaN,
    )
}
