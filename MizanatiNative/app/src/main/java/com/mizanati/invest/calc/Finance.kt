package com.mizanati.invest.calc

import com.mizanati.invest.data.*
import kotlin.math.*

object Finance {

    fun parseDate(s: String): Long? = try {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.isLenient = false
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        f.parse(s)?.time
    } catch (e: Exception) { null }

    fun monthsBetween(a: String, b: String): Int {
        val da = parseDate(a) ?: return 0
        val db = parseDate(b) ?: return 0
        val calA = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = da }
        val calB = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = db }
        return (calB.get(java.util.Calendar.YEAR) - calA.get(java.util.Calendar.YEAR)) * 12 +
            (calB.get(java.util.Calendar.MONTH) - calA.get(java.util.Calendar.MONTH))
    }

    fun addMonths(d: String, n: Int): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        parseDate(d)?.let { cal.timeInMillis = it }
        cal.add(java.util.Calendar.MONTH, n)
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return f.format(cal.time)
    }

    fun daysBetween(a: String, b: String): Int {
        val da = parseDate(a) ?: return 0
        val db = parseDate(b) ?: return 0
        return ((db - da) / 86400000L).toInt()
    }

    /** XIRR: money-weighted annualized return. Returns NaN if not computable. */
    fun xirr(cf: List<Pair<Long, Double>>): Double {
        if (cf.isEmpty()) return Double.NaN
        var neg = false; var pos = false
        var t0 = Long.MAX_VALUE
        cf.forEach { if (it.second < 0) neg = true else if (it.second > 0) pos = true; t0 = minOf(t0, it.first) }
        if (!neg || !pos) return Double.NaN
        val E = DoubleArray(cf.size) { (cf[it].first - t0) / 31536000000.0 }
        val V = DoubleArray(cf.size) { cf[it].second }
        fun g(r: Double): Double {
            val l = ln1p(r); var a = 0.0
            for (k in cf.indices) a += V[k] * exp(-E[k] * l)
            return a
        }
        var lo = -0.9999; var hi = 1e4
        val alo = g(lo); if (alo * g(hi) > 0) return Double.NaN
        for (it in 0 until 200) {
            val m = (lo + hi) / 2; val y = g(m)
            if (alo * y <= 0) hi = m else { lo = m }
            if (hi - lo < 1e-13) break
        }
        return (lo + hi) / 2
    }

    // ---- instrument valuation ----

    /** Cost basis of an instrument (positive buys, negative sells). */
    fun cost(i: Instrument): Double = i.tx.sumOf { it.amt }

    fun units(i: Instrument): Double = i.tx.filter { it.typ == "buy" || it.typ == "sell" }.sumOf { it.units }

    fun avgPrice(i: Instrument): Double {
        val u = units(i); return if (u > 0) cost(i) / u else 0.0
    }

    /** Current market value of an instrument in its own currency. */
    fun value(i: Instrument): Double = when (i.kind) {
        "gold" -> if (i.mode == "p" && i.price > 0) units(i) * i.price else i.cv
        "cert" -> {
            // capitalization / coupon accrual
            val u = units(i)
            if (u > 0 && i.price > 0) u * i.price
            else if (i.cv > 0) i.cv
            else {
                val m = monthsBetween(i.start.ifBlank { today() }, today())
                if (i.rate > 0 && m > 0) {
                    val base = cost(i)
                    if (i.cSum) base * Math.pow(1 + i.rate / 100.0 / 12.0, m.toDouble())
                    else base + base * i.rate / 100.0 / 12.0 * m
                } else cost(i)
            }
        }
        "prop" -> {
            if (i.cv > 0) i.cv
            else {
                val yrs = max(0.0, monthsBetween(i.buyD.ifBlank { today() }, today()) / 12.0)
                val p0 = if (i.price0 > 0) i.price0 else cost(i)
                p0 * (1 + i.ret / 100.0 * yrs) - (i.feePct / 100.0 * p0)
            }
        }
        else -> if (i.cv > 0) i.cv else cost(i)
    }

    data class InsStats(
        val cost: Double, val value: Double,
        val realized: Double, val unrealized: Double, val total: Double,
        val xr: Double, val ageDays: Int,
        val couponToDate: Double, val netRent: Double, val rentTotal: Double,
    )

    fun insStats(i: Instrument, fxNow: Double): InsStats {
        val cost = cost(i)
        val value = value(i)
        val realized = i.tx.filter { it.typ == "sell" || it.typ == "coupon" || it.typ == "maturity" || it.typ == "rent" }
            .sumOf { -it.amt }
        val unrealized = value - (cost + realized.coerceAtLeast(0.0)).let { if (realized > 0) cost else value - value }
        val tot = realized + (value - cost) - if (realized > 0) 0.0 else 0.0
        val flows = mutableListOf<Pair<Long, Double>>()
        i.tx.forEach { t -> parseDate(t.d)?.let { d -> flows.add(Pair(d, -t.amt)) } }
        parseDate(i.cvDate).let { d -> if (value > 0) flows.add(Pair(d ?: System.currentTimeMillis(), value)) }
        val xr = xirr(flows)
        val age = i.tx.minOfOrNull { daysBetween(it.d, today()) } ?: 0
        val coupon = if (i.kind == "cert" && i.rate > 0 && !i.cSum) {
            val m = monthsBetween(i.start.ifBlank { today() }, today())
            cost * i.rate / 100.0 / 12.0 * max(0, m)
        } else 0.0
        val rentNet = if (i.kind == "prop") i.rent - i.expM else 0.0
        val rentTotal = if (i.kind == "prop") rentNet * max(0, monthsBetween(i.rentStart.ifBlank { today() }, today())) else 0.0
        return InsStats(cost, value, realized, unrealized, tot, xr, age, coupon, rentNet, rentTotal)
    }

    fun pfTgt(p: Portfolio): Double = p.tAmt

    fun insTgt(i: Instrument): Double = if (i.tgtMode == "pct") i.tgtPct else i.tgt

    data class PfStats(
        val cost: Double, val value: Double,
        val realized: Double, val unrealized: Double, val total: Double,
        val pct: Double, val xr: Double, val ageDays: Int,
        val fxEffect: Double,
    )

    fun pfStats(p: Portfolio, ins: List<Instrument>, fxNow: Double): PfStats {
        val mine = ins.filter { it.pf == p.id }
        var cost = 0.0; var value = 0.0; var realized = 0.0
        var costU = 0.0; var valueU = 0.0; var totU = 0.0
        val flows = mutableListOf<Pair<Long, Double>>()
        val usd = p.cur == "USD"
        mine.forEach { i ->
            val st = insStats(i, fxNow)
            cost += st.cost; value += st.value; realized += st.realized
            costU += if (usd) st.cost else st.cost / fxNow
            valueU += if (usd) st.value else st.value / fxNow
            flows.addAll(stFlows(i, st, usd, fxNow))
        }
        val tot = value - cost + realized
        val fxEffect = if (usd) (tot - totU * fxNow) else (totU * fxNow - tot)
        val pct = if (cost > 0) tot / cost * 100 else Double.NaN
        val xr = xirr(flows)
        val age = mine.maxOfOrNull { insStats(it, fxNow).ageDays } ?: 0
        return PfStats(cost, value, realized, value - cost, tot, pct, xr, age, fxEffect)
    }

    private fun stFlows(i: Instrument, st: InsStats, usd: Boolean, fxNow: Double): List<Pair<Long, Double>> {
        val out = mutableListOf<Pair<Long, Double>>()
        i.tx.forEach { t ->
            parseDate(t.d)?.let { d -> out.add(Pair(d, -t.amt)) }
        }
        if (st.value > 0) {
            val d = parseDate(i.cvDate)
            out.add(Pair(d ?: System.currentTimeMillis(), st.value))
        }
        return out
    }

    // ---- totals ----

    data class Totals(
        val value: Double, val cost: Double, val profit: Double,
        val pct: Double, val xirr: Double,
        val valueUsd: Double, val usdPct: Double,
        val egpVal: Double, val usdVal: Double,
    )

    fun totals(s: AppState): Totals {
        var valEgp = 0.0; var valUsd = 0.0; var costT = 0.0
        s.pfs.filter { it.showHome && !it.isEmergency }.forEach { p ->
            val usd = p.cur == "USD"
            s.ins.filter { it.pf == p.id }.forEach { i ->
                val st = insStats(i, s.fx.now)
                if (usd) { valUsd += st.value; valEgp += st.value * s.fx.now }
                else { valEgp += st.value; valUsd += st.value / s.fx.now }
                costT += if (usd) st.cost * s.fx.now else st.cost
            }
        }
        val s2 = s.emergencyFund
        if (s2.include) {
            val dep = s2.transactions.filter { it.amt > 0 }.sumOf { it.amt }
            val wth = s2.transactions.filter { it.amt < 0 }.sumOf { -it.amt }
            val cur = if (s2.invested) s2.currentValue else dep - wth
            if (s2.currency == "USD") { valUsd += cur; valEgp += cur * s.fx.now }
            else { valEgp += cur; valUsd += cur / s.fx.now }
        }
        val total = valEgp
        val profit = total - costT
        val flows = mutableListOf<Pair<Long, Double>>()
        s.pfs.filter { !it.isEmergency }.forEach { p ->
            s.ins.filter { it.pf == p.id }.forEach { i ->
                i.tx.forEach { t -> parseDate(t.d)?.let { d ->
                    val amt = if (p.cur == "USD") t.amt * s.fx.now else t.amt
                    flows.add(Pair(d, -amt))
                } }
                val st = insStats(i, s.fx.now)
                if (st.value > 0) {
                    val v = if (p.cur == "USD") st.value * s.fx.now else st.value
                    flows.add(Pair(parseDate(i.cvDate) ?: System.currentTimeMillis(), v))
                }
            }
        }
        return Totals(
            value = total, cost = costT, profit = profit,
            pct = if (costT > 0) profit / costT * 100 else Double.NaN,
            xirr = xirr(flows),
            valueUsd = valUsd,
            usdPct = if (total > 0) valUsd * s.fx.now / total else 0.0,
            egpVal = valEgp, usdVal = valUsd,
        )
    }

    // ---- emergency fund ----

    fun efDeposits(ef: EmergencyFund) = ef.transactions.filter { it.amt > 0 }.sumOf { it.amt }
    fun efWithdrawals(ef: EmergencyFund) = ef.transactions.filter { it.amt < 0 }.sumOf { -it.amt }
    fun efBalance(ef: EmergencyFund): Double =
        if (ef.invested) ef.currentValue else efDeposits(ef) - efWithdrawals(ef)

    fun efXirr(ef: EmergencyFund): Double {
        val flows = mutableListOf<Pair<Long, Double>>()
        ef.transactions.forEach { t -> parseDate(t.d)?.let { d -> flows.add(Pair(d, -t.amt)) } }
        if (ef.invested && ef.currentValue > 0)
            flows.add(Pair(parseDate(ef.currentValueDate) ?: System.currentTimeMillis(), ef.currentValue))
        return xirr(flows)
    }

    fun efWeightedAgeDays(ef: EmergencyFund): Int {
        var num = 0.0; var den = 0.0
        ef.transactions.forEach { t ->
            val d = daysBetween(t.d, today())
            num += abs(t.amt) * d; den += abs(t.amt)
        }
        return if (den > 0) (num / den).toInt() else 0
    }

    // ---- formatting ----

    fun fmt(v: Double, digits: Int = 0): String {
        if (v.isNaN()) return "—"
        val sym = java.text.DecimalFormatSymbols(java.util.Locale.US)
        val pat = if (digits == 0) "#,##0" else "#,##0." + "0".repeat(digits)
        val df = java.text.DecimalFormat(pat, sym)
        return df.format(v)
    }

    fun pc(v: Double, digits: Int = 1): String = if (v.isNaN()) "—" else "${fmt(v, digits)}%"

    fun curSym(cur: String) = if (cur == "USD") "$" else "ج"
}
