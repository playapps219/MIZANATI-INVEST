package com.mizanati.invest.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val EF_PF_ID = "__mizanati_emergency_fund__"
const val EF_SEC_ID = "__mizanati_emergency_section__"
const val EF_INS_ID = "__mizanati_emergency_ledger__"

val COLS = listOf("#2f6df6", "#0e9f6e", "#e5871a", "#c026d3", "#0891b2", "#dc2626")

private const val UID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz"

fun uid(): String = buildString { repeat(6) { append(UID_CHARS[Random.nextInt(UID_CHARS.length)]) } }

fun mk(names: List<String>): JSONArray = jsonArrayOf(names.map { jobj("id" to uid(), "name" to it) })

fun mkP(name: String, cur: String, secs: List<String>, col: String, tRet: Double, id: String? = null): JSONObject =
    jobj("id" to (id ?: uid()), "name" to name, "cur" to cur, "col" to col, "tAmt" to 0, "tRet" to tRet, "secs" to mk(secs))

fun seed(): JSONObject = jobj(
    "split" to 80,
    "fx" to jobj("book" to 50, "bookDate" to ago(365), "now" to 50, "upd" to ""),
    "pfs" to jsonArrayOf(
        listOf(
            mkP("محفظة الدولار", "USD", listOf("دخل ثابت", "أسهم", "صناديق أسهم"), COLS[0], 8.0, "usd"),
            mkP("محفظة الجنيه", "EGP", listOf("دخل ثابت", "شهادات بنكية", "أسهم", "صناديق أسهم"), COLS[1], 19.0, "egp"),
            mkP("محفظة الذهب", "EGP", listOf("صناديق ذهب", "ذهب عيني"), "#d4a017", 15.0, "gold"),
            mkP("محفظة العقار", "EGP", listOf("صناديق عقار", "عقار عيني"), "#8b5cf6", 12.0, "re"),
        ),
    ),
    "ins" to JSONArray(),
    "v6" to 1,
    "emergencyFund" to jobj(
        "include" to false,
        "currency" to "EGP",
        "transactions" to JSONArray(),
        "invested" to false,
        "currentValue" to 0,
        "currentValueDate" to "",
        "valueInitialized" to false,
        "valueTxIds" to JSONArray(),
        "safetyThreshold" to 0,
        "safetyMonths" to 3,
        "monthlySpend" to 0,
        "safetyBudgetNeedsReview" to false,
        "coverageSource" to "allPortfolios",
        "coveragePortfolioIds" to JSONArray(),
        "coverageInstrumentIds" to JSONArray(),
        "coverageCurrency" to "fund",
        "inflationEnabled" to false,
        "inflationRate" to 0,
        "inflationCountry" to "EGY",
        "inflationCountryManual" to false,
        "inflationSource" to "manual",
        "inflationDataYear" to "",
        "forecastYears" to 1,
        "forecastPreset" to "1",
    ),
)

private fun finite(v: Any?): Boolean = jsNum(v).let { !it.isNaN() && !it.isInfinite() }

private fun JSONObject.isBool(k: String): Boolean = opt(k) is Boolean

private fun JSONObject.strArr(k: String): JSONArray = jsonArrayOf(optJSONArray(k)?.strings() ?: emptyList())

private fun JSONObject.blank(k: String): Boolean = opt(k).let { it == null || it == JSONObject.NULL || it == "" }

private fun assign(defaults: JSONObject, over: JSONObject?): JSONObject {
    if (over != null) for (k in over.keys()) defaults.put(k, over.opt(k))
    return defaults
}

fun migrate(S: JSONObject): JSONObject {
    val fx = S.obj("fx")
    if (!jsTruthy(S.opt("pfs"))) {
        val secs = S.optJSONObject("secs") ?: JSONObject()
        val req = S.optJSONObject("req") ?: JSONObject()
        S.put(
            "pfs",
            jsonArrayOf(
                listOf(
                    jobj(
                        "id" to "usd", "name" to "محفظة الدولار", "cur" to "USD", "col" to COLS[0], "tAmt" to 0,
                        "tRet" to req.num("usd").orElse(8.0), "secs" to (secs.optJSONArray("usd") ?: JSONArray()),
                    ),
                    jobj(
                        "id" to "egp", "name" to "محفظة الجنيه", "cur" to "EGP", "col" to COLS[1], "tAmt" to 0,
                        "tRet" to req.num("egp").orElse(19.0), "secs" to (secs.optJSONArray("egp") ?: JSONArray()),
                    ),
                ),
            ),
        )
        S.remove("secs")
    }
    val pfs = S.arr("pfs")
    S.arr("ins").objs().forEach { i ->
        val p = pfs.objs().firstOrNull { it.str("id") == i.str("pf") }
        if (p != null && p.str("cur") == "USD") {
            i.list("tx").forEach { t -> if (!(t.num("fx") > 1.5)) t.set("fx", fx.num("now")) }
        }
        if (i.num("cashVal") > 0 && !i.truthy("cvDate")) i.put("cvDate", "9999-12-31")
    }
    pfs.objs().forEach { p -> if (!p.isBool("showHome")) p.put("showHome", true) }
    if (!S.truthy("v6")) {
        if (pfs.objs().none { it.str("id") == "gold" }) {
            pfs.put(mkP("محفظة الذهب", "EGP", listOf("صناديق ذهب", "ذهب عيني"), "#d4a017", 15.0, "gold"))
        }
        if (pfs.objs().none { it.str("id") == "re" }) {
            pfs.put(mkP("محفظة العقار", "EGP", listOf("صناديق عقار", "عقار عيني"), "#8b5cf6", 12.0, "re"))
        }
        S.put("v6", 1)
    }

    val ef = S.optJSONObject("emergencyFund")
        ?: jobj("include" to false, "currency" to "EGP", "transactions" to JSONArray()).also { S.put("emergencyFund", it) }
    ef.put("include", ef.truthy("include"))
    ef.put("showHome", if (ef.isBool("showHome")) ef.optBoolean("showHome") else false)
    ef.put("currency", if (ef.str("currency") in listOf("EGP", "USD")) ef.str("currency") else "EGP")
    if (ef.optJSONArray("transactions") == null) ef.put("transactions", JSONArray())
    ef.list("transactions").forEachIndexed { i, t -> if (!t.truthy("id")) t.put("id", "ef-migrated-$i") }
    ef.put("invested", ef.truthy("invested"))
    ef.set("currentValue", if (finite(ef.opt("currentValue"))) max(0.0, ef.num("currentValue")) else 0.0)
    ef.put("currentValueDate", if (ef.truthy("currentValueDate")) ef.str("currentValueDate") else "")
    ef.put("valueInitialized", ef.truthy("valueInitialized"))
    if (ef.optJSONArray("valueTxIds") == null) ef.put("valueTxIds", JSONArray())
    ef.set("safetyThreshold", if (finite(ef.opt("safetyThreshold"))) max(0.0, ef.num("safetyThreshold")) else 0.0)
    ef.put(
        "safetyBudgetNeedsReview",
        if (ef.isBool("safetyBudgetNeedsReview")) ef.optBoolean("safetyBudgetNeedsReview")
        else !finite(ef.opt("monthlySpend")) && ef.num("safetyThreshold") > 0,
    )
    ef.set(
        "safetyMonths",
        if (finite(ef.opt("safetyMonths")) && ef.num("safetyMonths") > 0) min(120.0, ef.num("safetyMonths")) else 3.0,
    )
    ef.set(
        "monthlySpend",
        if (finite(ef.opt("monthlySpend")) && ef.num("monthlySpend") >= 0) ef.num("monthlySpend")
        else ef.num("safetyThreshold") / ef.num("safetyMonths"),
    )
    ef.set("safetyThreshold", ef.num("safetyMonths") * ef.num("monthlySpend"))
    ef.set(
        "forecastYears",
        if (finite(ef.opt("forecastYears")) && ef.num("forecastYears") > 0) min(50.0, ef.num("forecastYears")) else 1.0,
    )
    ef.put(
        "forecastPreset",
        if (ef.str("forecastPreset") in listOf("1", "2", "3", "5", "custom")) ef.str("forecastPreset")
        else if (ef.num("forecastYears") in listOf(1.0, 2.0, 3.0, 5.0)) ef.str("forecastYears") else "custom",
    )
    ef.put(
        "coverageSource",
        if (ef.str("coverageSource") in listOf("allPortfolios", "selectedPortfolios", "allInstruments", "selectedInstruments")) {
            ef.str("coverageSource")
        } else {
            "allPortfolios"
        },
    )
    ef.put("coverageCurrency", if (ef.str("coverageCurrency") in listOf("fund", "EGP", "USD")) ef.str("coverageCurrency") else "fund")
    ef.put("coveragePortfolioIds", ef.strArr("coveragePortfolioIds"))
    ef.put("coverageInstrumentIds", ef.strArr("coverageInstrumentIds"))
    ef.put("inflationEnabled", ef.truthy("inflationEnabled"))
    ef.set(
        "inflationRate",
        if (finite(ef.opt("inflationRate")) && ef.num("inflationRate") > -100 && ef.num("inflationRate") <= 1000) {
            ef.num("inflationRate")
        } else {
            0.0
        },
    )
    ef.put("inflationCountryManual", ef.truthy("inflationCountryManual"))
    ef.put(
        "inflationCountry",
        if (ef.str("inflationCountry") in listOf("EGY", "USA", "SAU", "ARE", "GBR", "CAN", "AUS")) ef.str("inflationCountry")
        else if (ef.str("currency") == "USD") "USA" else "EGY",
    )
    ef.put("inflationSource", if (ef.str("inflationSource") == "worldbank") "worldbank" else "manual")
    ef.put("inflationDataYear", if (ef.truthy("inflationDataYear")) ef.str("inflationDataYear") else "")

    val plans = S.optJSONArray("plans")?.objs() ?: emptyList()
    if (!S.truthy("finFreedomGoalsArchived")) {
        val archived = S.optJSONArray("archivedFutureGoals")?.let { a -> (0 until a.length()).map { a.opt(it) } } ?: emptyList()
        S.put("archivedFutureGoals", jsonArrayOf(archived + plans.filter { it.str("kind") == "goal" }))
        S.put("finFreedomGoalsArchived", true)
    }
    S.put("plans", jsonArrayOf(plans.filter { it.str("kind") != "goal" }))

    var ff = assign(
        jobj(
            "currency" to "EGP", "monthlySpend" to "", "retirementIncome" to "", "currentAge" to "", "retirementAge" to "",
            "planEndAge" to "95", "inflationRate" to "", "withdrawalRate" to 4, "monthlyContribution" to "",
            "returnMode" to "realized", "manualReturn" to "",
        ),
        S.optJSONObject("finFreedom"),
    )
    for (k in listOf(
        "monthlySpend", "retirementIncome", "currentAge", "retirementAge", "planEndAge", "inflationRate", "withdrawalRate",
        "monthlyContribution", "manualReturn",
    )) {
        if (ff.blank(k) || !finite(ff.opt(k))) ff.put(k, "") else ff.set(k, ff.num(k))
    }
    if (ff.str("currency") !in listOf("EGP", "USD")) ff.put("currency", "EGP")
    if (ff.str("returnMode") !in listOf("realized", "manual")) ff.put("returnMode", "realized")
    ff = assign(
        jobj(
            "currency" to "EGP", "fxMode" to "current", "portfolioIds" to null, "assetIds" to null, "monthlySpend" to 50000,
            "currentAge" to 35, "retirementAge" to 60, "planEndAge" to 95, "inflationRate" to 6, "inflationMode" to "manual",
            "inflationCountry" to "EGY", "inflationSource" to "manual", "inflationPeriod" to "", "withdrawalRate" to 4,
            "retirementIncome" to 0, "incomeGrowthMode" to "inflation", "incomeGrowthRate" to 0, "contributionAmount" to 10000,
            "contributionFrequency" to 1, "contributionGrowthMode" to "inflation", "contributionGrowthRate" to 0,
            "returnMode" to "actual", "manualReturn" to 8, "scenario" to "base", "returnConservative" to 4, "returnBase" to 8,
            "returnOptimistic" to 11,
        ),
        ff,
    )
    if (ff.opt("inflationRate") == "" || !finite(ff.opt("inflationRate"))) ff.put("inflationRate", 6)
    if (ff.str("returnMode") !in listOf("manual", "actual")) ff.put("returnMode", "actual")
    if (ff.str("fxMode") !in listOf("current", "historical", "average", "reported")) ff.put("fxMode", "current")
    for ((k, v) in listOf(
        "monthlySpend" to 50000, "currentAge" to 35, "retirementAge" to 60, "planEndAge" to 95, "withdrawalRate" to 4,
        "contributionAmount" to 10000, "contributionFrequency" to 1, "returnBase" to 8, "returnConservative" to 4,
        "returnOptimistic" to 11,
    )) {
        if (ff.blank(k)) ff.put(k, v)
    }
    if (ff.str("spendCurrency") !in listOf("EGP", "USD")) ff.put("spendCurrency", "EGP")
    if (ff.str("withdrawalChoice") !in listOf("4", "3.5", "3", "custom")) {
        ff.put("withdrawalChoice", if (ff.str("withdrawalRate") in listOf("3", "3.5", "4")) ff.str("withdrawalRate") else "custom")
    }
    S.put("finFreedom", ff)

    if (fx.optJSONArray("history") == null) fx.put("history", JSONArray())
    return S
}

fun syncEmergencyPortfolio(S: JSONObject) {
    val ef = S.optJSONObject("emergencyFund") ?: JSONObject()
    S.put("pfs", jsonArrayOf(S.list("pfs").filter { !it.truthy("isEmergency") && it.str("id") != EF_PF_ID }))
    S.put("ins", jsonArrayOf(S.list("ins").filter { !it.truthy("isEmergency") && it.str("id") != EF_INS_ID }))
    if (!ef.truthy("include")) return
    val cur = if (ef.str("currency") == "USD") "USD" else "EGP"
    val market = ef.truthy("invested")
    val value = Engine(S).emergencyMarketValue()
    val fxNow = (S.optJSONObject("fx")?.num("now") ?: Double.NaN).orElse(1.0)
    val tx = ef.list("transactions")
        .filter { it.truthy("date") && it.num("amount") > 0 }
        .map { t ->
            jobj(
                "id" to t.str("id").ifEmpty { uid() },
                "date" to t.str("date"),
                "type" to if (t.str("type") == "deposit") "buy" else "sell",
                "kind" to if (market) "cash" else "units",
                "amt" to t.num("amount"),
                "units" to t.num("amount"),
                "fx" to t.num("fx").orElse(fxNow),
            )
        }
    S.arr("pfs").put(
        jobj(
            "id" to EF_PF_ID, "name" to "صندوق الطوارئ", "cur" to cur, "col" to "#0e9f6e", "tAmt" to 0, "tRet" to 0,
            "secs" to jsonArrayOf(listOf(jobj("id" to EF_SEC_ID, "name" to "رصيد صندوق الطوارئ"))),
            "showHome" to true, "icon" to "🛟", "isEmergency" to true,
        ),
    )
    S.arr("ins").put(
        jobj(
            "id" to EF_INS_ID, "pf" to EF_PF_ID, "sec" to EF_SEC_ID, "name" to "رصيد صندوق الطوارئ", "kind" to "fund",
            "mode" to "p", "price" to 1, "cashVal" to value, "cvDate" to "9999-12-31", "emergencyValMode" to market,
            "tx" to jsonArrayOf(tx), "isEmergency" to true,
        ),
    )
}

fun persistableState(S: JSONObject): JSONObject {
    val o = JSONObject()
    for (k in S.keys()) o.put(k, S.opt(k))
    o.put("pfs", jsonArrayOf(S.list("pfs").filter { !it.truthy("isEmergency") }))
    o.put("ins", jsonArrayOf(S.list("ins").filter { !it.truthy("isEmergency") }))
    return o
}

fun isoNow(): String {
    val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date())
}

fun backupPayload(S: JSONObject): String =
    jobj("app" to "Mizanati-Invest", "version" to 2, "exportedAt" to isoNow(), "data" to persistableState(S)).toString(2)

fun validateBackupState(data: JSONObject?): Boolean {
    if (data == null) return false
    val pfs = data.optJSONArray("pfs") ?: return false
    val ins = data.optJSONArray("ins") ?: return false
    if (pfs.length() > 1000 || ins.length() > 100000) return false
    return pfs.objs().size == pfs.length() && ins.objs().size == ins.length()
}

class Store(private val file: File) {
    var S: JSONObject = JSONObject()
        private set

    init {
        load()
    }

    fun load() {
        val raw = try {
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
        var s = try {
            if (raw.isNullOrEmpty()) null else JSONObject(raw)
        } catch (e: Exception) {
            null
        }
        if (s == null || !(s.truthy("secs") || s.truthy("pfs"))) s = seed()
        S = migrate(s)
    }

    fun save(): Boolean = writeVerified(persistableState(S).toString())

    fun import(data: JSONObject): Boolean {
        val fx = data.obj("fx")
        if (fx.optJSONArray("history") == null) fx.put("history", JSONArray())
        if (!writeVerified(persistableState(data).toString())) return false
        load()
        return true
    }

    fun reset() {
        file.delete()
        load()
    }

    private fun writeVerified(json: String): Boolean = try {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json)
        if (tmp.readText() != json) throw IllegalStateException("verify")
        if (!tmp.renameTo(file)) {
            file.writeText(json)
            tmp.delete()
        }
        true
    } catch (e: Exception) {
        false
    }
}
