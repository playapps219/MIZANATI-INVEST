package com.mizanati.invest.network

import com.mizanati.invest.data.Fx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// FX fetching with the same 4-source fallback chain as the original app.

object FxClient {

    private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"

    private suspend fun httpText(url: String, timeoutMs: Int = 12000): String =
        withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", UA)
                conn.setRequestProperty("Accept", "text/plain")
                if (conn.responseCode !in 200..299) throw Exception("http ${conn.responseCode}")
                conn.inputStream.bufferedReader().readText()
            } finally { conn.disconnect() }
        }

    /** Try every source in order; return USD→EGP rate or null. */
    suspend fun fetchUsdEgp(): Double? = withContext(Dispatchers.IO) {
        val stamp = System.currentTimeMillis()
        val sources: List<Pair<String, (JSONObject) -> Double?>> = listOf(
            "https://open.er-api.com/v6/latest/USD" to { j -> j.optJSONObject("rates")?.optDouble("EGP")?.takeIf { !it.isNaN() } },
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json" to { j -> j.optJSONObject("usd")?.optDouble("egp")?.takeIf { !it.isNaN() } },
            "https://latest.currency-api.pages.dev/v1/currencies/usd.json" to { j -> j.optJSONObject("usd")?.optDouble("egp")?.takeIf { !it.isNaN() } },
            "https://api.frankfurter.app/latest?from=USD&to=EGP" to { j -> j.optJSONObject("rates")?.optDouble("EGP")?.takeIf { !it.isNaN() } },
        )
        for ((url, parse) in sources) {
            try {
                val u = url + (if (url.contains('?')) '&' else '?') + "_=" + stamp
                val v = parse(JSONObject(httpText(u)))
                if (v != null && v > 1 && v < 1000) return@withContext Math.round(v * 100.0) / 100.0
            } catch (_: Exception) { /* next source */ }
        }
        null
    }

    /** Historical USD→EGP for a given yyyy-MM-dd date (frankfurter). */
    suspend fun fetchHistorical(date: String): Double? = withContext(Dispatchers.IO) {
        try {
            val j = JSONObject(httpText("https://api.frankfurter.app/$date?from=USD&to=EGP"))
            j.optJSONObject("rates")?.optDouble("EGP")?.takeIf { it > 0 && !it.isNaN() }
        } catch (_: Exception) { null }
    }

    /** Auto refresh: at most every 30 minutes, same rule as the original. */
    suspend fun autoUpdateIfNeeded(fx: Fx, force: Boolean = false): Fx? {
        if (!fx.auto) return null
        if (!force && System.currentTimeMillis() - fx.auAt < 30L * 60000L) return null
        val rate = fetchUsdEgp() ?: return null
        fx.now = rate
        fx.upd = stamp() + " (تلقائي)"
        fx.auAt = System.currentTimeMillis()
        return fx
    }

    fun stamp(): String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        .format(java.util.Date())
}
