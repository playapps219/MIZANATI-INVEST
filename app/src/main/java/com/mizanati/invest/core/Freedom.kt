package com.mizanati.invest.core

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp

class Freedom(val S: JSONObject, val now: Double) {

    val q: JSONObject get() = S.optJSONObject("finFreedom") ?: JSONObject()

    // ---- item accessors (mirror ff2Items/ff2ItemsRaw) ----

    fun items(): List<JSONObject> {
        val arr = q.optJSONArray("items") ?: JSONArray()
        return arr.objs().filter { it.str("id").isNotEmpty() }
    }

    fun goalSet(): Set<String> = S.optJSONArray("plans")?.objs()
        ?.filter { it.str("kind") == "goal" }
        ?.mapNotNull { it.str("id") }
        ?.toSet() ?: emptySet()

    fun activeItems(): List<JSONObject> {
        val gs = goalSet()
        return items().filter { it.str("id") !in gs }
    }

    fun item(id: String): JSONObject? = items().firstOrNull { it.str("id") == id }

    fun newItem(kind: String): JSONObject = JSONObject()
        .put("id", "ff" + System.currentTimeMillis())
        .put("kind", kind)
        .put("name", if (kind == "asset") "أصل" else "التزام")
        .put("v", 0.0)
        .put("cur", "EGP")
        .put("g", 0.0)
        .put("mi", 0.0)

    fun saveItems(items: List<JSONObject>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        q.put("items", arr)
        S.put("finFreedom", q)
    }

    fun removeItem(id: String) = saveItems(items().filter { it.str("id") != id })

    // ---- conversions (mirror ff2V6Convert/ff2V5Convert/gConv) ----

    fun fx(): Double = (S.optJSONObject("fx")?.num("now") ?: Double.NaN).orElse(50.0)

    fun toUsd(v: Double, cur: String): Double = gConv(v, cur, "USD", fx())

    fun toEgp(v: Double, cur: String): Double = gConv(v, cur, "EGP", fx())

    // ---- projections (mirror ff2AssetValue/ff2Mode/ff2Rate) ----

    fun itemValue(i: JSONObject, years: Double): Double {
        val v0 = i.nz("v")
        val g = i.nz("g") / 100.0
        val mi = i.nz("mi") * 12.0 * years
        val grown = v0 * exp(g * years)
        return (grown + mi).coerceAtLeast(0.0)
    }

    fun assetsAt(years: Double): Double =
        activeItems().filter { it.str("kind") != "liab" }
            .sumOf { toUsd(itemValue(it, years), it.str("cur")) }

    fun liabsAt(years: Double): Double =
        activeItems().filter { it.str("kind") == "liab" }
            .sumOf { toUsd(itemValue(it, years), it.str("cur")) }

    fun netAt(years: Double): Double = assetsAt(years) - liabsAt(years)

    // portfolio projection: current value grown at blended XIRR + monthly contribution
    fun portfolioAt(engine: Engine, years: Double): Double {
        val p0 = engine.totalPortfolioUSD
        return portfolioAt(engine, years, p0, 0.0, monthlyContrib())
    }

    fun portfolioAt(engine: Engine, years: Double, p0: Double, rate: Double, monthly: Double): Double {
        val n = years * 12.0
        if (rate == 0.0) return p0 + monthly * n
        val mr = exp(rate / 12.0) - 1.0
        return (p0 * exp(rate * years)) + (monthly * ((exp(mr * n) - 1.0) / mr))
    }

    fun blendRate(c: Calc): Double =
        listOf(c.xr, c.xu).filter { it.isFinite() }.let { l -> if (l.isEmpty()) 0.0 else l.average() }

    // ---- freedom target (mirror ff2Factor/ff2Actual) ----

    fun spendUsd(): Double = toUsd(q.nz("spend"), q.str("spendCur").ifEmpty { "EGP" })

    fun target(years: Double): Double {
        val f = q.nz("factor").orElse(25.0).coerceIn(1.0, 100.0)
        return spendUsd() * f + liabsAt(years)
    }

    fun yearsToFreedom(p0: Double, rate: Double, monthly: Double, target0: Double): Double {
        if (target0 <= 0) return Double.NaN
        if (p0 >= target0) return 0.0
        if (rate <= 0 && monthly <= 0) return Double.NaN
        val mr = exp(rate / 12.0) - 1.0
        var lo = 0.0
        var hi = 80.0
        repeat(60) {
            val mid = (lo + hi) / 2.0
            val v = p0 * exp(rate * mid) + monthly * ((exp(mr * mid * 12.0) - 1.0) / (if (mr > 0) mr else 1.0))
            if (v < target0) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    fun monthlyContrib(): Double {
        val mi = activeItems().filter { it.str("kind") != "liab" && it.truthy("fromPf") }
            .sumOf { toUsd(it.nz("mi"), it.str("cur")) }
        return toUsd(S.nz("monthly"), "USD") + mi
    }

    fun monthlySpendEgp(): Double = toEgp(q.nz("spend"), q.str("spendCur").ifEmpty { "EGP" })

    fun currentNetUsd(engine: Engine): Double = netAt(0.0) + engine.totalPortfolioUSD
}
