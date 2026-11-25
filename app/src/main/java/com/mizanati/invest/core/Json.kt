package com.mizanati.invest.core

import org.json.JSONArray
import org.json.JSONObject

fun jsNum(v: Any?): Double = when (v) {
    null -> Double.NaN
    JSONObject.NULL -> 0.0
    is Number -> v.toDouble()
    is Boolean -> if (v) 1.0 else 0.0
    is String -> v.trim().let { if (it.isEmpty()) 0.0 else it.toDoubleOrNull() ?: Double.NaN }
    else -> Double.NaN
}

fun jsTruthy(v: Any?): Boolean = when (v) {
    null, JSONObject.NULL -> false
    is Boolean -> v
    is Number -> v.toDouble().let { !it.isNaN() && it != 0.0 }
    is String -> v.isNotEmpty()
    else -> true
}

fun JSONObject.num(k: String): Double = jsNum(opt(k))

fun JSONObject.nz(k: String): Double = num(k).let { if (it.isNaN()) 0.0 else it }

fun JSONObject.str(k: String): String = when (val v = opt(k)) {
    null, JSONObject.NULL -> ""
    is Double -> if (v == Math.floor(v) && !v.isInfinite() && Math.abs(v) < 1e15) v.toLong().toString() else v.toString()
    else -> v.toString()
}

fun JSONObject.truthy(k: String): Boolean = jsTruthy(opt(k))

fun JSONObject.obj(k: String): JSONObject {
    val v = optJSONObject(k)
    if (v != null) return v
    val n = JSONObject()
    put(k, n)
    return n
}

fun JSONObject.arr(k: String): JSONArray {
    val v = optJSONArray(k)
    if (v != null) return v
    val n = JSONArray()
    put(k, n)
    return n
}

fun JSONObject.set(k: String, v: Double): JSONObject {
    if (v.isNaN() || v.isInfinite()) put(k, JSONObject.NULL) else put(k, v)
    return this
}

fun JSONArray.objs(): List<JSONObject> {
    val out = ArrayList<JSONObject>(length())
    for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
    return out
}

fun JSONArray.strings(): List<String> {
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val v = opt(i)
        if (v != null && v != JSONObject.NULL) out.add(v.toString())
    }
    return out
}

fun jsonArrayOf(items: Iterable<Any?>): JSONArray {
    val a = JSONArray()
    for (x in items) a.put(x)
    return a
}

fun jobj(vararg kv: Pair<String, Any?>): JSONObject {
    val o = JSONObject()
    for ((k, v) in kv) {
        when (v) {
            null -> o.put(k, JSONObject.NULL)
            is Double -> o.set(k, v)
            else -> o.put(k, v)
        }
    }
    return o
}
