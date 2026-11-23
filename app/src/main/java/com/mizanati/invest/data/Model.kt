package com.mizanati.invest.data

import org.json.JSONArray
import org.json.JSONObject

// Data model mirroring the original app's state schema (localStorage key "pf4").

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
    var cur: String,          // "USD" | "EGP"
    var col: String,          // hex color
    var tAmt: Double = 0.0,   // target/allocated amount
    var tRet: Double = 0.0,   // target annual return %
    var secs: MutableList<Sec> = mutableListOf(),
    var showHome: Boolean = true,
)

data class Tx(
    var id: String,
    var d: String,            // date yyyy-MM-dd
    var amt: Double,          // contribution (+) / withdrawal (-)
    var fx: Double = 0.0,     // fx at transaction time (USD wallets)
    var note: String = "",
)

data class Instrument(
    var id: String,
    var pf: String,           // portfolio id
    var name: String,
    var kind: String = "",    // sector / type
    var tx: MutableList<Tx> = mutableListOf(),
    var cv: Double = 0.0,     // current value (latest)
    var cvDate: String = "",
    var ret: Double = 0.0,
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
    var valueTxIds: MutableList<String> = mutableListOf(),
    var safetyThreshold: Double = 0.0,
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
    var by: String,           // target date
    var saved: Double = 0.0,
)

data class AppState(
    var split: Int = 80,
    var fx: Fx = Fx(),
    var pfs: MutableList<Portfolio> = mutableListOf(),
    var ins: MutableList<Instrument> = mutableListOf(),
    var emergencyFund: EmergencyFund = EmergencyFund(),
    var goals: MutableList<Goal> = mutableListOf(),
    var theme: String = "light",   // "light" | "dark"
    var v6: Int = 1,
)

fun uid(): String = java.util.UUID.randomUUID().toString().substring(0, 8)
fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    .format(java.util.Date())

object Seed {
    val COLS = listOf("#2f6df6", "#0e9f6e", "#e5871a", "#c026d3", "#0891b2", "#dc2626")

    fun state(): AppState {
        val ago365 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 365L * 86400000L))
        fun p(id: String, name: String, cur: String, col: String, tRet: Double, secs: List<String>) =
            Portfolio(id = id, name = name, cur = cur, col = col, tRet = tRet,
                secs = secs.map { Sec(uid(), it) }.toMutableList())
        val s = AppState()
        s.fx = Fx(book = 50.0, bookDate = ago365, now = 50.0)
        s.pfs = mutableListOf(
            p("usd", "محفظة الدولار", "USD", COLS[0], 8.0, listOf("دخل ثابت", "أسهم", "صناديق أسهم")),
            p("egp", "محفظة الجنيه", "EGP", COLS[1], 19.0, listOf("دخل ثابت", "شهادات بنكية", "أسهم", "صناديق أسهم")),
            p("gold", "محفظة الذهب", "EGP", "#d4a017", 15.0, listOf("صناديق ذهب", "ذهب عيني")),
            p("re", "محفظة العقار", "EGP", "#8b5cf6", 12.0, listOf("صناديق عقار", "عقار عيني")),
        )
        return s
    }
}

// ---- JSON (de)serialization, same field names as the original app ----

object StateJson {

    fun toJson(s: AppState): JSONObject {
        val o = JSONObject()
        o.put("split", s.split)
        o.put("v6", s.v6)
        o.put("theme", s.theme)
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
                    put("showHome", p.showHome)
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
                    put("cv", i.cv); put("cvDate", i.cvDate); put("ret", i.ret)
                    put("tx", JSONArray().apply {
                        i.tx.forEach { t ->
                            put(JSONObject().apply {
                                put("id", t.id); put("d", t.d); put("amt", t.amt)
                                put("fx", t.fx); put("note", t.note)
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
                })
            }
        })
        o.put("emergencyFund", JSONObject().apply {
            val ef = s.emergencyFund
            put("include", ef.include); put("currency", ef.currency)
            put("invested", ef.invested); put("currentValue", ef.currentValue)
            put("currentValueDate", ef.currentValueDate); put("valueInitialized", ef.valueInitialized)
            put("safetyThreshold", ef.safetyThreshold); put("safetyMonths", ef.safetyMonths)
            put("monthlySpend", ef.monthlySpend); put("showHome", ef.showHome)
            put("forecastYears", ef.forecastYears)
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
        s.split = o.optInt("split", 80)
        s.v6 = o.optInt("v6", 1)
        s.theme = o.optString("theme", "light")
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
                            note = t.optString("note"),
                        ))
                    }
                }
                s.ins.add(Instrument(
                    id = e.optString("id"), pf = e.optString("pf"),
                    name = e.optString("name"), kind = e.optString("kind"),
                    tx = txs, cv = e.optDouble("cv", 0.0),
                    cvDate = e.optString("cvDate"), ret = e.optDouble("ret", 0.0),
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
                ))
            }
        }
        o.optJSONObject("emergencyFund")?.let { e ->
            val ef = EmergencyFund(
                include = e.optBoolean("include"), currency = e.optString("currency", "EGP"),
                invested = e.optBoolean("invested"), currentValue = e.optDouble("currentValue", 0.0),
                currentValueDate = e.optString("currentValueDate"),
                valueInitialized = e.optBoolean("valueInitialized"),
                safetyThreshold = e.optDouble("safetyThreshold", 0.0),
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
