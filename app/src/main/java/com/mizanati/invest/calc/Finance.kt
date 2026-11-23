package com.mizanati.invest.calc

import com.mizanati.invest.data.Instrument
import com.mizanati.invest.data.Portfolio
import com.mizanati.invest.data.AppState
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln1p

// Financial calculations ported from the original app.

object Finance {

    /** Annualized money-weighted return (XIRR) over cashflows; cf.v signed, cf.t epoch ms. */
    fun xirr(cf: List<Pair<Long, Double>>): Double {
        if (cf.isEmpty()) return Double.NaN
        var neg = false; var pos = false
        var t0 = Long.MAX_VALUE
        cf.forEach { if (it.second < 0) neg = true else if (it.second > 0) pos = true; t0 = minOf(t0, it.first) }
        if (!neg || !pos) return Double.NaN
        val E = DoubleArray(cf.size) { (cf[it].first - t0) / 31536000000.0 }
        val V = DoubleArray(cf.size) { cf[it].second }
        fun g(r: Double): Double {
            val l = ln1p(r)
            var a = 0.0
            for (k in cf.indices) a += V[k] * exp(-E[k] * l)
            return a
        }
        var lo = -0.9999; var hi = 1e4
        var a = g(lo)
        if (a * g(hi) > 0) return Double.NaN
        for (it in 0 until 200) {
            val m = (lo + hi) / 2
            val y = g(m)
            if (a * y <= 0) hi = m else { lo = m; a = y }
            if (hi - lo < 1e-13) break
        }
        return (lo + hi) / 2
    }

    /** Invested cost of an instrument: sum of signed contributions (fx-adjusted by caller). */
    fun invested(i: Instrument, fxNow: Double): Double =
        i.tx.sumOf { t -> if (t.fx > 1.5) t.fx else fxNow } * 0.0 + // fx bookkeeping handled by caller
            i.tx.sumOf { it.amt }

    /** Current value of an instrument; falls back to contributed cost when no cv set. */
    fun currentValue(i: Instrument): Double =
        if (i.cv > 0) i.cv else i.tx.sumOf { it.amt }

    fun instrumentFlows(i: Instrument, fxNow: Double): List<Pair<Long, Double>> {
        val out = mutableListOf<Pair<Long, Double>>()
        i.tx.forEach { t ->
            val ms = parseDate(t.d) ?: return@forEach
            val amt = t.amt
            if (i.portfolioCurIsUsd) out.add(ms to amt) else out.add(ms to amt)
        }
        // terminal value as positive flow at cv date
        val cv = currentValue(i)
        if (cv > 0) {
            val d = if (i.cvDate.isNotBlank() && i.cvDate != "9999-12-31") parseDate(i.cvDate) else null
            out.add((d ?: System.currentTimeMillis()) to cv)
        }
        return out
    }

    private var Instrument.portfolioCurIsUsd: Boolean
        get() = false
        set(v) {}

    fun parseDate(s: String): Long? = try {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        f.parse(s)?.time
    } catch (e: Exception) { null }

    /** Portfolio aggregated stats. */
    data class PfStats(
        val invested: Double,
        val value: Double,
        val profit: Double,
        val profitPct: Double,
        val xirr: Double,
    )

    fun pfStats(p: Portfolio, ins: List<Instrument>, fxNow: Double, toEgp: (Double, String) -> Double): PfStats {
        val mine = ins.filter { it.pf == p.id }
        var invested = 0.0; var value = 0.0
        val flows = mutableListOf<Pair<Long, Double>>()
        mine.forEach { i ->
            val v = currentValue(i)
            invested += toEgp(i.tx.sumOf { it.amt }, p.cur)
            value += toEgp(v, p.cur)
            flows.addAll(instrumentFlows(i, fxNow).map { (t, v2) -> t to toEgp(v2, p.cur) })
        }
        val profit = value - invested
        val pct = if (invested > 0) profit / invested * 100 else Double.NaN
        val x = xirr(flows)
        return PfStats(invested, value, profit, pct, if (abs(x) < 1e5) x * 100 else Double.NaN)
    }

    fun totalValue(s: AppState, toEgp: (Double, String) -> Double): Double =
        s.pfs.filter { it.showHome }.sumOf { p ->
            toEgp(s.ins.filter { it.pf == p.id }.sumOf { currentValue(it) }, p.cur)
        }

    fun totalInvested(s: AppState, toEgp: (Double, String) -> Double): Double =
        s.pfs.filter { it.showHome }.sumOf { p ->
            toEgp(s.ins.filter { it.pf == p.id }.sumOf { it.tx.sumOf { t -> t.amt } }, p.cur)
        }

    fun fxStats(fx: com.mizanati.invest.data.Fx): Pair<Double, Double> {
        val egpDown = if (fx.book > 0) (fx.now - fx.book) / fx.book * 100 else Double.NaN
        return egpDown to fx.now
    }

    fun fmt(v: Double, digits: Int = 0): String {
        if (v.isNaN()) return "—"
        val sym = java.text.DecimalFormatSymbols(java.util.Locale.US)
        val df = java.text.DecimalFormat("#,##0.${"#".repeat(digits)}", sym)
        return df.format(v)
    }

    fun pc(v: Double): String = if (v.isNaN()) "—" else "${fmt(v, 1)}%"
}
