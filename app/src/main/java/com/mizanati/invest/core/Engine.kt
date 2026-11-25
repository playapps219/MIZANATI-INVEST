package com.mizanati.invest.core

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln1p
import kotlin.math.max
import kotlin.math.min

class Flow(val t: Double, val v: Double)

class Calc {
    var cost = 0.0
    var costU = 0.0
    var real = 0.0
    var realU = 0.0
    var buys = 0.0
    var buysU = 0.0
    var value = 0.0
    var valU = 0.0
    var cf: MutableList<Flow> = ArrayList()
    var cfU: MutableList<Flow> = ArrayList()
    var first: Double? = null
    var num = 0.0
    var den = 0.0
    var err = false
    var u = 0.0
    var avg = 0.0
    var matured = false
    var x: Map<String, Double> = emptyMap()
    var unr = 0.0
    var unrU = 0.0
    var tot = 0.0
    var totU = 0.0
    var fxe = 0.0
    var age = 0.0
    var xr = Double.NaN
    var xu = Double.NaN
}

fun Double.orElse(d: Double): Double = if (isNaN() || this == 0.0) d else this

fun JSONObject.list(k: String): List<JSONObject> = optJSONArray(k)?.objs() ?: emptyList()

fun xirr(cf: List<Flow>): Double {
    val n = cf.size
    var neg = false
    var pos = false
    var t0 = Double.POSITIVE_INFINITY
    for (c in cf) {
        if (c.v < 0) neg = true else if (c.v > 0) pos = true
        if (c.t < t0) t0 = c.t
    }
    if (!neg || !pos) return Double.NaN
    val e = DoubleArray(n) { (cf[it].t - t0) / 31536e6 }
    val v = DoubleArray(n) { cf[it].v }
    fun g(r: Double): Double {
        val l = ln1p(r)
        var a = 0.0
        for (k in 0 until n) a += v[k] * exp(-e[k] * l)
        return a
    }
    var lo = -.9999
    var hi = 1e4
    var a = g(lo)
    if (a * g(hi) > 0) return Double.NaN
    for (it in 0 until 200) {
        val m = (lo + hi) / 2
        val y = g(m)
        if (a * y <= 0) hi = m else {
            lo = m
            a = y
        }
        if (hi - lo < 1e-13) break
    }
    return (lo + hi) / 2
}

fun gConv(v: Double, from: String, to: String, rate: Double): Double {
    if (from == to) return v
    val fx = if (rate > 0) rate else 1.0
    return if (from == "EGP") v / fx else v * fx
}

fun profitRatio(r: Calc): Double =
    if (r.cost > 0) (r.real + r.unr) / r.cost else if (r.buys > 0) r.real / r.buys else Double.NaN

private fun addMd(s: String, m: Double): Double = if (m.isNaN() || m.isInfinite()) Double.NaN else addM(s, m.toInt())

class Engine(val S: JSONObject, val now: Double = System.currentTimeMillis().toDouble()) {

    val pfs: List<JSONObject> = S.list("pfs")
    val ins: List<JSONObject> = S.list("ins")
    val fn: Double = (S.optJSONObject("fx")?.num("now") ?: Double.NaN).orElse(1.0)
    val ef: JSONObject = S.optJSONObject("emergencyFund") ?: JSONObject()

    fun P(id: String): JSONObject = pfs.firstOrNull { it.str("id") == id } ?: pfs.firstOrNull() ?: JSONObject()

    fun insOf(p: JSONObject): List<JSONObject> = p.str("id").let { id -> ins.filter { it.str("pf") == id } }

    fun calc(i: JSONObject): Calc {
        val p = P(i.str("pf"))
        val E = p.str("cur") == "EGP"
        val N = now
        val fn = fn
        fun cv(a: Double, x: Double) = if (E) a / x else a * x
        val r = Calc()
        val kind = i.str("kind")
        if (kind == "prop") {
            val A = i.nz("amt") + i.nz("buyCosts")
            val fx0 = i.num("fx").orElse(fn)
            val start = i.str("start")
            val st = D(start)
            val rent = i.nz("rent")
            val eM = i.nz("expM")
            val eY = i.nz("expY")
            val cf = ArrayList<Flow>()
            cf.add(Flow(st, -A))
            var inc = 0.0
            var exp = 0.0
            val rentStart = i.str("rentStart").ifEmpty { start }
            for (k in 1..1200) {
                val t = addM(rentStart, k)
                if (t > N) break
                inc += rent
                cf.add(Flow(t, rent))
            }
            for (k in 1..1200) {
                val t = addM(start, k)
                if (t > N) break
                exp += eM
                cf.add(Flow(t, -eM))
            }
            for (k in 1..100) {
                val t = addM(start, 12 * k)
                if (t > N) break
                exp += eY
                cf.add(Flow(t, -eY))
            }
            val net = inc - exp
            r.cost = A
            r.costU = cv(A, fx0)
            r.value = if (i.num("curVal") > 0) i.num("curVal") else A
            r.real = net
            r.realU = cv(net, fn)
            r.buys = A
            r.buysU = cv(A, fx0)
            r.first = st
            r.num = A * (N - st) / 864e5
            r.den = A
            r.cf = cf
            r.cfU = cf.mapIndexedTo(ArrayList()) { j, c -> if (j != 0) Flow(c.t, cv(c.v, fn)) else Flow(c.t, -cv(A, fx0)) }
            val amt = i.num("amt")
            val hasAmt = i.truthy("amt")
            r.x = mapOf(
                "inc" to inc,
                "exp" to exp,
                "net" to net,
                "gy" to if (hasAmt) rent * 12 / amt else Double.NaN,
                "ny" to if (hasAmt) (rent * 12 - eM * 12 - eY) / amt else Double.NaN,
                "cg" to r.value - A,
            )
            r.valU = cv(r.value, fn)
            return r
        }
        if (kind == "cert") {
            val A = i.nz("amt")
            val fx0 = i.num("fx").orElse(fn)
            val term = i.num("term")
            val m = i.num("freq").orElse(term.orElse(1.0))
            val per = A * i.num("rate") / 100 * m / 12
            val netP = per - per * i.num("feePct") / 100 - i.nz("feeFix")
            val n = max(1.0, floor(term / m + 1e-9))
            val start = i.str("start")
            val st = D(start)
            val pays = ArrayList<Double>()
            var k = 1
            while (k <= n) {
                pays.add(addMd(start, k * m))
                k++
            }
            val rec = pays.filter { it <= N }
            val matT = addMd(start, term)
            val mat = matT <= N
            var acc = 0.0
            if (!mat && A != 0.0) {
                val l = if (rec.isNotEmpty()) rec[rec.size - 1] else st
                val nx = (pays.getOrNull(rec.size) ?: Double.NaN).orElse(matT)
                acc = max(0.0, min(1.0, (N - l) / (nx - l))) * netP
            }
            r.cost = if (mat) 0.0 else A
            r.costU = if (mat) 0.0 else cv(A, fx0)
            r.value = if (mat) 0.0 else A + acc
            r.real = rec.size * netP
            r.realU = cv(rec.size * netP, fn) + (if (mat) cv(A, fn) - cv(A, fx0) else 0.0)
            r.buys = A
            r.buysU = cv(A, fx0)
            r.first = st
            r.num = A * (N - st) / 864e5
            r.den = A
            r.cf = ArrayList<Flow>().apply {
                add(Flow(st, -A))
                rec.forEach { add(Flow(it, netP)) }
            }
            r.cfU = ArrayList<Flow>().apply {
                add(Flow(st, -cv(A, fx0)))
                rec.forEach { add(Flow(it, cv(netP, fn))) }
            }
            if (mat) {
                r.cf.add(Flow(matT, A))
                r.cfU.add(Flow(matT, cv(A, fn)))
            }
            val full = ArrayList<Flow>()
            full.add(Flow(st, -A))
            pays.forEach { full.add(Flow(it, netP)) }
            full.add(Flow(matT, A))
            r.matured = mat
            r.x = mapOf(
                "per" to per,
                "netP" to netP,
                "n" to n,
                "rc" to rec.size.toDouble(),
                "matT" to matT,
                "acc" to acc,
                "yn" to if (A != 0.0) netP * (12 / m) / A else Double.NaN,
                "xf" to xirr(full),
            )
        } else {
            var u = 0.0
            var cu = 0.0
            var cuU = 0.0
            var cc = 0.0
            var ccU = 0.0
            var adj = 0.0
            val cvd = i.str("cvDate").ifEmpty { "9999-12-31" }
            val cashVal = i.num("cashVal")
            i.list("tx").sortedWith { a, b -> a.str("date").compareTo(b.str("date")) }.forEach { t ->
                val fx = t.num("fx").orElse(fn)
                val date = t.str("date")
                val tt = D(date)
                val b = t.str("type") == "buy"
                val amt = t.num("amt")
                val units = t.num("units")
                val byUnits = t.str("kind") == "units"
                val f0 = r.first
                if (f0 == null || tt < f0) r.first = tt
                if (b) {
                    r.buys += amt
                    r.buysU += cv(amt, fx)
                    r.num += amt * (N - tt) / 864e5
                    r.den += amt
                    if (byUnits) {
                        u += units
                        cu += amt
                        cuU += cv(amt, fx)
                    } else {
                        cc += amt
                        ccU += cv(amt, fx)
                        if (date > cvd) adj += amt
                    }
                } else if (byUnits) {
                    if (units > u + 1e-9) {
                        r.err = true
                        return@forEach
                    }
                    val fr = units / u
                    val rc = cu * fr
                    val rU = cuU * fr
                    r.real += amt - rc
                    r.realU += cv(amt, fx) - rU
                    cu -= rc
                    cuU -= rU
                    u -= units
                } else {
                    val base = if (cashVal > 0) cashVal + amt else cc
                    val fr = if (base > 0) min(1.0, amt / base) else 0.0
                    val rc = cc * fr
                    val rU = ccU * fr
                    r.real += amt - rc
                    r.realU += cv(amt, fx) - rU
                    cc -= rc
                    ccU -= rU
                    if (date > cvd) adj -= amt
                }
                r.cf.add(Flow(tt, if (b) -amt else amt))
                r.cfU.add(Flow(tt, if (b) -cv(amt, fx) else cv(amt, fx)))
            }
            if (u < 1e-9) {
                u = 0.0
                cu = 0.0
                cuU = 0.0
            }
            if (cc < 1e-6) {
                cc = 0.0
                ccU = 0.0
            }
            val vu = if (u > 0) (if (i.str("mode") == "t") i.nz("total") else u * i.nz("price")) else 0.0
            val vc = if (i.truthy("isEmergency") && i.truthy("emergencyValMode")) max(0.0, i.nz("cashVal"))
            else if (cc > 0) (if (cashVal > 0) max(0.0, cashVal + adj) else cc) else 0.0
            r.value = vu + vc
            r.cost = cu + cc
            r.costU = cuU + ccU
            r.u = u
            r.avg = if (u != 0.0) cu / u else 0.0
        }
        r.valU = cv(r.value, fn)
        return r
    }

    fun fin(r: Calc, E: Boolean): Calc {
        r.unr = r.value - r.cost
        r.unrU = r.valU - r.costU
        r.tot = r.real + r.unr
        r.totU = r.realU + r.unrU
        r.fxe = r.totU - (if (E) r.tot / fn else r.tot * fn)
        r.age = if (r.den != 0.0 && !r.den.isNaN()) r.num / r.den else 0.0
        val a = ArrayList(r.cf)
        val b = ArrayList(r.cfU)
        if (r.value > 0) {
            a.add(Flow(now, r.value))
            b.add(Flow(now, r.valU))
        }
        r.xr = xirr(a)
        r.xu = xirr(b)
        return r
    }

    fun agg(L: List<JSONObject>, E: Boolean): Calc {
        val r = Calc()
        L.forEach { i ->
            val c = calc(i)
            r.cost += c.cost
            r.costU += c.costU
            r.real += c.real
            r.realU += c.realU
            r.buys += c.buys
            r.buysU += c.buysU
            r.value += c.value
            r.valU += c.valU
            r.num += c.num
            r.den += c.den
            r.cf.addAll(c.cf)
            r.cfU.addAll(c.cfU)
        }
        return fin(r, E)
    }

    fun sym(p: JSONObject): String = if (p.str("cur") == "USD") "$" else "ج"

    fun osym(p: JSONObject): String = if (p.str("cur") == "USD") "ج" else "$"

    fun oname(p: JSONObject): String = if (p.str("cur") == "USD") "بالجنيه" else "بالدولار"

    val totalPortfolioUSD: Double by lazy {
        pfs.fold(0.0) { sum, p ->
            val e = p.str("cur") == "EGP"
            sum + insOf(p).fold(0.0) { a, i -> a + fin(calc(i), e).value } / (if (e) fn else 1.0)
        }
    }

    fun pctAllocated(): Double =
        ins.fold(0.0) { a, i -> a + if (i.str("tgtMode") == "pct") max(0.0, i.nz("tgtPct")) else 0.0 }

    fun insTgt(i: JSONObject): Double {
        if (i.str("tgtMode") == "pct") {
            val p = P(i.str("pf"))
            return totalPortfolioUSD * (max(0.0, i.nz("tgtPct")) / 100) * (if (p.str("cur") == "EGP") fn else 1.0)
        }
        return if (i.num("tgt") > 0) i.num("tgt") else 0.0
    }

    fun pfSum(p: JSONObject): Double = insOf(p).fold(0.0) { a, i -> a + insTgt(i) }

    fun hasInstrumentTargets(p: JSONObject): Boolean =
        insOf(p).any { it.str("tgtMode") == "pct" || it.nz("tgt") > 0 }

    fun pfTgt(p: JSONObject): Double =
        if (hasInstrumentTargets(p)) pfSum(p) else if (p.num("tAmt") > 0) p.num("tAmt") else 0.0

    val emergencyTx: List<JSONObject> get() = ef.list("transactions")

    fun emergencyLedgerBalance(items: List<JSONObject>, allowNegative: Boolean = false): Double? {
        var bal = 0.0
        val q = items.withIndex().sortedWith { a, b ->
            a.value.str("date").compareTo(b.value.str("date")).let { if (it != 0) it else a.index - b.index }
        }
        for ((_, t) in q) {
            bal += (if (t.str("type") == "deposit") 1 else -1) * t.nz("amount")
            if (!allowNegative && bal < -.000001) return null
        }
        return bal
    }

    fun emergencyFlow(t: JSONObject): Double = when (t.str("type")) {
        "deposit" -> t.nz("amount")
        "withdrawal" -> -t.nz("amount")
        else -> 0.0
    }

    fun emergencyDeposits(items: List<JSONObject> = emergencyTx): Double =
        items.filter { it.str("type") == "deposit" }.fold(0.0) { a, t -> a + t.nz("amount") }

    fun emergencyWithdrawals(items: List<JSONObject> = emergencyTx): Double =
        items.filter { it.str("type") == "withdrawal" }.fold(0.0) { a, t -> a + t.nz("amount") }

    fun emergencyRawMarketValue(): Double {
        val txs = emergencyTx
        if (!ef.truthy("invested")) return (emergencyLedgerBalance(txs, true) ?: 0.0).orElse(0.0)
        val included = (ef.optJSONArray("valueTxIds")?.strings() ?: emptyList()).toHashSet()
        val later = txs.filter { !included.contains(it.str("id")) }.fold(0.0) { a, t -> a + emergencyFlow(t) }
        return ef.nz("currentValue") + later
    }

    fun emergencyMarketValue(): Double = max(0.0, emergencyRawMarketValue())

    fun emergencyProfit(): Double =
        if (ef.truthy("invested")) emergencyMarketValue() + emergencyWithdrawals() - emergencyDeposits() else 0.0

    fun emergencyBelowSafety(): Boolean {
        val limit = ef.nz("safetyThreshold")
        return limit > 0 && emergencyMarketValue() + 1e-7 < limit
    }

    fun growthAvgFx(items: List<JSONObject>, nowFx: Double): Double {
        var egp = 0.0
        var usd = 0.0
        fun add(amt: Double, cur: String, rate: Double) {
            if (!(amt > 0)) return
            val fx = if (rate > 0) rate else nowFx
            if (cur == "EGP") {
                egp += amt
                usd += amt / fx
            } else {
                usd += amt
                egp += amt * fx
            }
        }
        items.forEach { i ->
            val cur = P(i.str("pf")).str("cur")
            val kind = i.str("kind")
            if (kind == "prop" || kind == "cert") {
                add(i.nz("amt") + (if (kind == "prop") i.nz("buyCosts") else 0.0), cur, i.num("fx").orElse(nowFx))
            } else {
                i.list("tx").filter { it.str("type") == "buy" }.forEach { t -> add(t.nz("amt"), cur, t.num("fx").orElse(nowFx)) }
            }
        }
        return if (usd > 0) egp / usd else nowFx
    }

    fun syncAutoPfTargets(): Boolean {
        var changed = false
        pfs.forEach { p ->
            if (hasInstrumentTargets(p)) {
                val v = pfSum(p)
                val cur = p.num("tAmt")
                if (cur.isNaN() || cur.isInfinite() || abs(cur - v) > .005) {
                    p.set("tAmt", tf(v, 2).toDoubleOrNull() ?: v)
                    changed = true
                }
            }
        }
        return changed
    }
}
