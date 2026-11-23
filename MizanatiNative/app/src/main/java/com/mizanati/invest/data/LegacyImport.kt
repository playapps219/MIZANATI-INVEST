package com.mizanati.invest.data

import org.json.JSONObject

object LegacyImport {
    fun normalize(source: JSONObject): JSONObject {
        val result = JSONObject(source.toString())
        result.optJSONArray("ins")?.let { instruments ->
            for (index in 0 until instruments.length()) {
                val item = instruments.getJSONObject(index)
                item.optJSONArray("tx")?.let { transactions ->
                    for (position in 0 until transactions.length()) {
                        val tx = transactions.getJSONObject(position)
                        if (!tx.has("d")) tx.put("d", tx.optString("date"))
                        if (!tx.has("typ")) {
                            val type = tx.optString("type", "buy")
                            tx.put("typ", type)
                            val amount = kotlin.math.abs(tx.optDouble("amt", 0.0))
                            tx.put("amt", if (type in listOf("sell", "coupon", "maturity", "rent")) -amount else amount)
                        }
                    }
                }
                if (!item.has("cv")) item.put("cv", item.optDouble("cashVal", item.optDouble("curVal", 0.0)))
                if (!item.has("termM")) item.put("termM", item.optInt("term", 0))
                if (!item.has("pmtM")) item.put("pmtM", item.optInt("freq", 1))
                if (!item.has("price0")) item.put("price0", item.optDouble("amt", 0.0))
                if (!item.has("buyD")) item.put("buyD", item.optString("start"))
                if (item.optString("mode") == "t") {
                    item.put("mode", "v")
                    if (!item.has("cvDate")) item.put("cvDate", "9999-12-31")
                    item.put("cv", item.optDouble("total", 0.0))
                }
                if (item.optDouble("cv", 0.0) > 0 && item.optString("cvDate").isBlank()) item.put("cvDate", "9999-12-31")
            }
        }
        return result
    }
}
