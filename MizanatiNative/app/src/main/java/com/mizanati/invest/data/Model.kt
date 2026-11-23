package com.mizanati.invest.data

import org.json.JSONArray
import org.json.JSONObject

data class Fx(
    var book: Double = 50.0,
    var bookDate: String = "",
    var now: Double = 50.0,
    var upd: String = "",
    var auto: Boolean = true,
    var auAt: Long = 0L,
)

data class Sec(var id: String, var name: String)

data class Portfolio(
    var id: String,
    var name: String,
    var cur: String = "EGP",
    var col: String = "#2f6df6",
    var tAmt: Double = 0.0,
    var tRet: Double = 0.0,
    var secs: MutableList<Sec> = mutableListOf(),
    var showHome: Boolean = true,
    var isEmergency: Boolean = false,
    var split: Int? = null,
)

data class Tx(
    var id: String,
    var d: String,
    var amt: Double,
    var fx: Double = 0.0,
    var note: String = "",
    var typ: String = "buy",       // buy | sell | coupon | maturity | rent | fee
    var units: Double = 0.0,       // grams/documents where relevant
)

data class Instrument(
    var id: String,
    var pf: String,
    var name: String,
    var kind: String = "fund",     // fund | gold | cert | prop
    var sec: String = "",
    var tx: MutableList<Tx> = mutableListOf(),
    var cv: Double = 0.0,
    var cvDate: String = "",
    var ret: Double = 0.0,
    var tgt: Double = 0.0,         // target amount
    var tgtMode: String = "amt",   // amt | pct | off
    var tgtPct: Double = 0.0,
    var price: Double = 0.0,       // gold gram / cert doc price
    var mode: String = "v",        // v = value entry, p = price entry
    // cert fields
    var rate: Double = 0.0,
    var termM: Int = 0,
    var start: String = "",
    var cSum: Boolean = false,
    var pmtM: Int = 0,
    // prop fields
    var rent: Double = 0.0,
    var rentStart: String = "",
    var expM: Double = 0.0,
    var buyD: String = "",
    var price0: Double = 0.0,
    var feePct: Double = 0.0,
)

data class EfTx(var id: String, var d: String, var amt: Double, var note: String = "")

data class EmergencyFund(
    var include: Boolean = false,
    var currency: String = "EGP",
    var transactions: MutableList<EfTx> = mutableListOf(),
    var invested: Boolean = false,
    var currentValue: Double = 0.0,
    var currentValueDate: String = "",
    var valueInitialized: Boolean = false,
    var safetyMonths: Int = 3,
    var monthlySpend: Double = 0.0,
    var showHome: Boolean = false,
    var forecastYears: Int = 1,
)

data class Goal(
    var id: String,
    var name: String,
    var target: Double,
    var cur: String,
    var by: String,
    var saved: Double = 0.0,
    var pf: String = "",
    var note: String = "",
)

data class PayPlan(
    var id: String,
    var name: String,
    var pf: String,
    var ins: String,
    var pmt: Double,          // monthly payment
    var n: Int,               // months
    var start: String,
    var cur: String,
)

data class AppState(
    var split: Int = 80,
    var fx: Fx = Fx(),
    var pfs: MutableList<Portfolio> = mutableListOf(),
    var ins: MutableList<Instrument> = mutableListOf(),
    var emergencyFund: EmergencyFund = EmergencyFund(),
    var goals: MutableList<Goal> = mutableListOf(),
    var plans: MutableList<PayPlan> = mutableListOf(),
    var theme: String = "light",
    var v6: Int = 1,
    var inflation: Double = 0.0,
    var inflationMonthly: Double = 0.0,
    var inflationPeriod: String = "",
)

fun uid(): String = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8)
fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

object Seed {
    val COLS = listOf("#2f6df6", "#0e9f6e", "#e5871a", "#c026d3", "#0891b2", "#dc2626")

    fun state(): AppState {
        val ago365 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 365L * 86400000L))
        val s = AppState()
        s.fx = Fx(book = 50.0, bookDate = ago365, now = 50.0)
        fun p(id: String, name: String, cur: String, col: String, tRet: Double, secs: List<String>) =
            Portfolio(id = id, name = name, cur = cur, col = col, tRet = tRet,
                secs = secs.map { Sec(uid(), it) }.toMutableList())
        s.pfs = mutableListOf(
            p("usd", "محفظة الدولار", "USD", COLS[0], 8.0, listOf("دخل ثابت", "أسهم", "صناديق أسهم")),
            p("egp", "محفظة الجنيه", "EGP", COLS[1], 19.0, listOf("دخل ثابت", "شهادات بنكية", "أسهم", "صناديق أسهم")),
            p("gold", "محفظة الذهب", "EGP", "#d4a017", 15.0, listOf("صناديق ذهب", "ذهب عيني")),
            p("prop", "محفظة العقار", "EGP", "#8b5cf6", 12.0, listOf("صناديق عقار", "عقار عيني")),
        )
        return s
    }
}

object StateJson {

    fun toJson(s: AppState): JSONObject {
        val o = JSONObject()
        o.put("split", s.split); o.put("v6", s.v6); o.put("theme", s.theme)
        o.put("inflation", s.inflation); o.put("inflationMonthly", s.inflationMonthly)
        o.put("inflationPeriod", s.inflationPeriod)
        o.put("fx", JSONObject().apply {
            put("book", s.fx.book); put("bookDate", s.fx.bookDate)
            put("now", s.fx.now); put("upd", s.fx.upd)
            put("auto", s.fx.auto); put("auAt", s.fx.auAt)
        })
        o.put("pfs", JSONArray().apply {
            s.pfs.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("name", p.name); put("cur", p.cur)
                    put("col", p.col); put("tAmt", p.tAmt); put("tRet", p.tRet)
                    put("showHome", p.showHome); put("isEmergency", p.isEmergency)
                    p.split?.let { put("split", it) }
                    put("secs", JSONArray().apply {
                        p.secs.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
                    })
                })
            }
        })
        o.put("ins", JSONArray().apply {
            s.ins.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id); put("pf", i.pf); put("name", i.name); put("kind", i.kind)
                    put("sec", i.sec); put("cv", i.cv); put("cvDate", i.cvDate); put("ret", i.ret)
                    put("tgt", i.tgt); put("tgtMode", i.tgtMode); put("tgtPct", i.tgtPct)
                    put("price", i.price); put("mode", i.mode)
                    put("rate", i.rate); put("termM", i.termM); put("start", i.start)
                    put("cSum", i.cSum); put("pmtM", i.pmtM)
                    put("rent", i.rent); put("rentStart", i.rentStart); put("expM", i.expM)
                    put("buyD", i.buyD); put("price0", i.price0); put("feePct", i.feePct)
                    put("tx", JSONArray().apply {
                        i.tx.forEach { t ->
                            put(JSONObject().apply {
                                put("id", t.id); put("d", t.d); put("amt", t.amt)
                                put("fx", t.fx); put("note", t.note); put("typ", t.typ); put("units", t.units)
                            })
                        }
                    })
                })
            }
        })
        o.put("goals", JSONArray().apply {
            s.goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id); put("name", g.name); put("target", g.target)
                    put("cur", g.cur); put("by", g.by); put("saved", g.saved)
                    put("pf", g.pf); put("note", g.note)
                })
            }
        })
        o.put("plans", JSONArray().apply {
            s.plans.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("pf", c.pf); put("ins", c.ins)
                    put("pmt", c.pmt); put("n", c.n); put("start", c.start); put("cur", c.cur)
                })
            }
        })
        o.put("emergencyFund", JSONObject().apply {
            val ef = s.emergencyFund
            put("include", ef.include); put("currency", ef.currency)
            put("invested", ef.invested); put("currentValue", ef.currentValue)
            put("currentValueDate", ef.currentValueDate); put("valueInitialized", ef.valueInitialized)
            put("safetyMonths", ef.safetyMonths); put("monthlySpend", ef.monthlySpend)
            put("showHome", ef.showHome); put("forecastYears", ef.forecastYears)
            put("transactions", JSONArray().apply {
                ef.transactions.forEach { t ->
                    put(JSONObject().put("id", t.id).put("d", t.d).put("amt", t.amt).put("note", t.note))
                }
            })
        })
        return o
    }

    fun fromJson(o: JSONObject): AppState {
        val s = Seed.state()
        s.split = o.optInt("split", 80); s.v6 = o.optInt("v6", 1)
        s.theme = o.optString("theme", "light")
        s.inflation = o.optDouble("inflation", 0.0)
        s.inflationMonthly = o.optDouble("inflationMonthly", 0.0)
        s.inflationPeriod = o.optString("inflationPeriod", "")
        o.optJSONObject("fx")?.let { f ->
            s.fx = Fx(
                book = f.optDouble("book", 50.0), bookDate = f.optString("bookDate"),
                now = f.optDouble("now", 50.0), upd = f.optString("upd"),
                auto = f.optBoolean("auto", true), auAt = f.optLong("auAt", 0L),
            )
        }
        o.optJSONArray("pfs")?.let { arr ->
            s.pfs.clear()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val secs = mutableListOf<Sec>()
                p.optJSONArray("secs")?.let { sa ->
                    for (j in 0 until sa.length()) {
                        val e = sa.getJSONObject(j)
                        secs.add(Sec(e.optString("id"), e.optString("name")))
                    }
                }
                s.pfs.add(Portfolio(
                    id = p.optString("id"), name = p.optString("name"),
                    cur = p.optString("cur", "EGP"), col = p.optString("col", "#2f6df6"),
                    tAmt = p.optDouble("tAmt", 0.0), tRet = p.optDouble("tRet", 0.0),
                    secs = secs, showHome = p.optBoolean("showHome", true),
                    isEmergency = p.optBoolean("isEmergency", false),
                    split = if (p.has("split")) p.optInt("split") else null,
                ))
            }
        }
        o.optJSONArray("ins")?.let { arr ->
            s.ins.clear()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val txs = mutableListOf<Tx>()
                e.optJSONArray("tx")?.let { ta ->
                    for (j in 0 until ta.length()) {
                        val t = ta.getJSONObject(j)
                        txs.add(Tx(
                            id = t.optString("id"), d = t.optString("d"),
                            amt = t.optDouble("amt", 0.0), fx = t.optDouble("fx", 0.0),
                            note = t.optString("note"), typ = t.optString("typ", "buy"),
                            units = t.optDouble("units", 0.0),
                        ))
                    }
                }
                s.ins.add(Instrument(
                    id = e.optString("id"), pf = e.optString("pf"),
                    name = e.optString("name"), kind = e.optString("kind", "fund"),
                    sec = e.optString("sec"), tx = txs,
                    cv = e.optDouble("cv", 0.0), cvDate = e.optString("cvDate"),
                    ret = e.optDouble("ret", 0.0),
                    tgt = e.optDouble("tgt", 0.0),
                    tgtMode = e.optString("tgtMode", "amt"),
                    tgtPct = e.optDouble("tgtPct", 0.0),
                    price = e.optDouble("price", 0.0), mode = e.optString("mode", "v"),
                    rate = e.optDouble("rate", 0.0), termM = e.optInt("termM", 0),
                    start = e.optString("start"), cSum = e.optBoolean("cSum", false),
                    pmtM = e.optInt("pmtM", 0),
                    rent = e.optDouble("rent", 0.0), rentStart = e.optString("rentStart"),
                    expM = e.optDouble("expM", 0.0), buyD = e.optString("buyD"),
                    price0 = e.optDouble("price0", 0.0), feePct = e.optDouble("feePct", 0.0),
                ))
            }
        }
        o.optJSONArray("goals")?.let { arr ->
            s.goals.clear()
            for (i in 0 until arr.length()) {
                val g = arr.getJSONObject(i)
                s.goals.add(Goal(
                    id = g.optString("id"), name = g.optString("name"),
                    target = g.optDouble("target", 0.0), cur = g.optString("cur", "EGP"),
                    by = g.optString("by"), saved = g.optDouble("saved", 0.0),
                    pf = g.optString("pf"), note = g.optString("note"),
                ))
            }
        }
        o.optJSONArray("plans")?.let { arr ->
            s.plans.clear()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                s.plans.add(PayPlan(
                    id = c.optString("id"), name = c.optString("name"),
                    pf = c.optString("pf"), ins = c.optString("ins"),
                    pmt = c.optDouble("pmt", 0.0), n = c.optInt("n", 0),
                    start = c.optString("start"), cur = c.optString("cur", "EGP"),
                ))
            }
        }
        o.optJSONObject("emergencyFund")?.let { e ->
            val ef = EmergencyFund(
                include = e.optBoolean("include"), currency = e.optString("currency", "EGP"),
                invested = e.optBoolean("invested"), currentValue = e.optDouble("currentValue", 0.0),
                currentValueDate = e.optString("currentValueDate"),
                valueInitialized = e.optBoolean("valueInitialized"),
                safetyMonths = e.optInt("safetyMonths", 3),
                monthlySpend = e.optDouble("monthlySpend", 0.0),
                showHome = e.optBoolean("showHome"), forecastYears = e.optInt("forecastYears", 1),
            )
            e.optJSONArray("transactions")?.let { ta ->
                for (j in 0 until ta.length()) {
                    val t = ta.getJSONObject(j)
                    ef.transactions.add(EfTx(t.optString("id"), t.optString("d"),
                        t.optDouble("amt", 0.0), t.optString("note")))
                }
            }
            s.emergencyFund = ef
        }
        return s
    }
}
