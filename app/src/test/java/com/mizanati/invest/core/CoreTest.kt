package com.mizanati.invest.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CoreTest {

    private val YEAR = 31536e6

    private fun eq(a: Double, b: Double, eps: Double = 1e-6) =
        if (a.isNaN() && b.isNaN()) true else abs(a - b) <= eps

    // ---- Json.kt ----

    @Test
    fun jsNum_variants() {
        assertTrue(eq(jsNum(null), Double.NaN))
        assertTrue(eq(jsNum(JSONObject.NULL), 0.0))
        assertTrue(eq(jsNum(7), 7.0))
        assertTrue(eq(jsNum(true), 1.0))
        assertTrue(eq(jsNum(false), 0.0))
        assertTrue(eq(jsNum(" 12.5 "), 12.5))
        assertTrue(eq(jsNum(""), 0.0))
        assertTrue(eq(jsNum("abc"), Double.NaN))
    }

    @Test
    fun nz_convertsNanToZero() {
        val o = JSONObject("{\"a\":\"x\"}")
        assertTrue(eq(o.nz("a"), 0.0))
        assertTrue(eq(JSONObject("{\"a\":3.5}").nz("a"), 3.5))
    }

    @Test
    fun str_integerDouble_hasNoDecimalPart() {
        // stored as a Kotlin Double (Android org.json), not parsed from text
        assertEquals("5", JSONObject().put("k", 5.0).str("k"))
        assertEquals("2026-01-01", JSONObject("{\"k\":\"2026-01-01\"}").str("k"))
        assertEquals("", JSONObject().str("missing"))
    }

    @Test
    fun truthy_rules() {
        assertFalse(jsTruthy(null))
        assertFalse(jsTruthy(0.0))
        assertTrue(jsTruthy(1.0))
        assertTrue(jsTruthy("x"))
        assertFalse(jsTruthy(""))
        assertTrue(jsTruthy(this))
    }

    @Test
    fun set_writesNullForNonFinite() {
        val o = JSONObject().set("a", Double.NaN)
        assertTrue(o.get("a") == JSONObject.NULL)
        assertTrue(eq(JSONObject().set("b", 2.0).num("b"), 2.0))
    }

    @Test
    fun objs_skipsNonObjects() {
        val a = JSONArray().put(JSONObject("{\"x\":1}")).put("text").put(JSONObject.NULL)
        assertEquals(1, a.objs().size)
    }

    @Test
    fun strings_skipsNulls() {
        val a = JSONArray().put("a").put(JSONObject.NULL).put("b")
        assertEquals(listOf("a", "b"), a.strings())
    }

    @Test
    fun orElse_fallsBackOnNaNAndZero() {
        assertTrue(eq(Double.NaN.orElse(9.0), 9.0))
        assertTrue(eq(0.0.orElse(9.0), 9.0))
        assertTrue(eq(3.0.orElse(9.0), 3.0))
    }

    // ---- Engine.kt pure helpers ----

    @Test
    fun xirr_simpleOneYearReturn() {
        val r = xirr(listOf(Flow(0.0, -1000.0), Flow(YEAR, 1100.0)))
        assertTrue(eq(r, 0.1, 1e-5))
    }

    @Test
    fun xirr_noSignChange_isNaN() {
        assertTrue(xirr(listOf(Flow(0.0, 100.0), Flow(YEAR, 110.0))).isNaN())
        assertTrue(xirr(emptyList()).isNaN())
    }

    @Test
    fun gConv_rules() {
        assertTrue(eq(gConv(10.0, "USD", "USD", 50.0), 10.0))
        assertTrue(eq(gConv(100.0, "EGP", "USD", 50.0), 2.0))
        assertTrue(eq(gConv(2.0, "USD", "EGP", 50.0), 100.0))
        assertTrue(eq(gConv(100.0, "EGP", "USD", 0.0), 100.0))
    }

    @Test
    fun profitRatio_prefersCostBasis() {
        val c = Calc().apply { cost = 200.0; real = 10.0; unr = 30.0; buys = 50.0 }
        assertTrue(eq(profitRatio(c), 0.2))
        val c2 = Calc().apply { cost = 0.0; buys = 100.0; real = 25.0 }
        assertTrue(eq(profitRatio(c2), 0.25))
        assertTrue(profitRatio(Calc()).isNaN())
    }

    // ---- Engine emergency fund ----

    private fun engineWith(vararg tx: Pair<String, Double>, safety: Double = 0.0) =
        Engine(
            jobj(
                "fx" to jobj("now" to 50.0),
                "emergencyFund" to jobj(
                    "safetyThreshold" to safety,
                    "transactions" to jsonArrayOf(
                        tx.map { jobj("type" to it.first, "amount" to it.second) },
                    ),
                ),
            ),
            now = D("2026-06-30"),
        )

    @Test
    fun emergencyLedgerBalance_sumsDepositsMinusWithdrawals() {
        val e = engineWith("deposit" to 100.0, "deposit" to 50.0, "withdrawal" to 30.0)
        assertTrue(eq(e.emergencyLedgerBalance(e.emergencyTx)!!, 120.0))
        assertTrue(eq(e.emergencyDeposits(), 150.0))
        assertTrue(eq(e.emergencyWithdrawals(), 30.0))
    }

    @Test
    fun emergencyLedgerBalance_overdrawReturnsNull() {
        val e = engineWith("deposit" to 100.0, "withdrawal" to 150.0)
        assertNull(e.emergencyLedgerBalance(e.emergencyTx))
        assertTrue(eq(e.emergencyLedgerBalance(e.emergencyTx, allowNegative = true)!!, -50.0))
    }

    @Test
    fun emergencyBelowSafety_comparesMarketValueToThreshold() {
        val e = engineWith("deposit" to 100.0, safety = 200.0)
        assertTrue(e.emergencyBelowSafety())
        val e2 = engineWith("deposit" to 300.0, safety = 200.0)
        assertFalse(e2.emergencyBelowSafety())
        val e3 = engineWith("deposit" to 300.0, safety = 0.0)
        assertFalse(e3.emergencyBelowSafety())
    }

    // ---- Format.kt ----

    @Test
    fun dateParseAndAddMonths() {
        assertTrue(eq(D("2026-01-15"), D("2026-01-01") + 14 * 864e5))
        assertTrue(D("not-a-date").isNaN())
        assertTrue(D("2026-1-1").isNaN())
        val oneMonth = addM("2026-01-15", 1)
        assertTrue(eq(oneMonth, D("2026-02-15")))
    }

    @Test
    fun fxStats_fromBookToNow() {
        val e = Engine(
            jobj(
                "fx" to jobj("now" to 110.0, "book" to 100.0, "bookDate" to "2026-06-29"),
            ),
            now = D("2026-06-30"),
        )
        val s = e.fxStats()
        assertTrue(s.ok)
        assertTrue(eq(s.days, 1.0))
        assertTrue(eq(s.usdUp, 0.1))
        assertTrue(eq(s.egpDown, 1.0 - 100.0 / 110.0, 1e-9))
        assertTrue(s.ann.isNaN())
    }
}

class FreedomTest {

    private fun S(fx: Double = 50.0): JSONObject = JSONObject().put("fx", JSONObject().put("now", fx))

    @Test
    fun itemValue_growsAndAddsMonthly() {
        val f = Freedom(S(), 0.0)
        val i = JSONObject().put("v", 1000.0).put("g", 10.0).put("mi", 0.0)
        assertTrue(f.itemValue(i, 1.0) > 1000.0)
        val i2 = JSONObject().put("v", 1000.0).put("g", 0.0).put("mi", 10.0)
        assertTrue(abs(f.itemValue(i2, 1.0) - 1120.0) < 1e-6)
    }

    @Test
    fun toUsd_and_toEgp_roundTrip() {
        val f = Freedom(S(fx = 50.0), 0.0)
        assertTrue(abs(f.toUsd(100.0, "EGP") - 2.0) < 1e-9)
        assertTrue(abs(f.toEgp(2.0, "USD") - 100.0) < 1e-9)
        assertTrue(abs(f.toUsd(f.toEgp(7.0, "USD"), "EGP") - 7.0) < 1e-9)
    }

    @Test
    fun netAt_subtractsLiabilities() {
        val s = S()
        val ff = JSONObject().put(
            "items",
            jsonArrayOf(listOf(
                jobj("id" to "a", "kind" to "asset", "v" to 500.0, "cur" to "USD"),
                jobj("id" to "l", "kind" to "liab", "v" to 100.0, "cur" to "USD"),
            ),
            ),
        )
        s.put("finFreedom", ff)
        val f = Freedom(s, 0.0)
        assertTrue(abs(f.netAt(0.0) - 400.0) < 1e-9)
    }

    @Test
    fun yearsToFreedom_findsYearsByBisection() {
        val f = Freedom(S(), 0.0)
        // continuous compounding: 100k at 10% reaches 200k in ln(2)/0.1 = 6.93 y
        val y = f.yearsToFreedom(100000.0, 0.10, 0.0, 200000.0)
        assertTrue(abs(y - 6.9315) < 0.01)
    }

    @Test
    fun yearsToFreedom_unreachable_returnsNaN() {
        val f = Freedom(S(), 0.0)
        assertTrue(f.yearsToFreedom(100.0, 0.0, 0.0, 200.0).isNaN())
    }

    @Test
    fun goalArchived_itemsAreExcluded() {
        val s = S()
        val ff = JSONObject().put("items", jsonArrayOf(listOf(jobj("id" to "g", "kind" to "asset", "v" to 5.0))))
        s.put("finFreedom", ff)
        s.put("plans", jsonArrayOf(listOf(jobj("id" to "g", "kind" to "goal"))))
        val f = Freedom(s, 0.0)
        assertEquals(0, f.activeItems().size)
        assertEquals(1, f.items().size)
    }
}
