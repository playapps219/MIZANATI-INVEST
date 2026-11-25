package com.mizanati.invest.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

const val LRM = "\u200E"

private val ISO = Regex("""^(\d{4})-(\d{2})-(\d{2})""")
private val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"

fun z2(n: Int): String = n.toString().padStart(2, '0')

fun tf(x: Double, d: Int): String {
    if (x.isNaN() || x.isInfinite()) return if (x.isNaN()) "NaN" else if (x > 0) "Infinity" else "-Infinity"
    val s = BigDecimal(x).setScale(d, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return if (s == "-0") "0" else s
}

fun f(x: Double, d: Int = 0): String {
    if (x.isNaN() || x.isInfinite()) return "-"
    val bd = BigDecimal(x).setScale(d, RoundingMode.HALF_UP)
    val df = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    df.maximumFractionDigits = d
    df.minimumFractionDigits = 0
    df.roundingMode = RoundingMode.HALF_UP
    val s = df.format(bd)
    return (if (x < 0) LRM else "") + s
}

fun pc(x: Double): String =
    if (x.isNaN() || x.isInfinite()) "—" else (if (x < 0) LRM else "") + tf(x * 100, 1) + "%"

fun parseIso(s: String?): IntArray? {
    val m = ISO.find(s ?: "") ?: return null
    val y = m.groupValues[1].toInt()
    val mo = m.groupValues[2].toInt()
    val d = m.groupValues[3].toInt()
    if (mo !in 1..12 || d !in 1..31) return null
    return intArrayOf(y, mo, d)
}

fun D(s: String?): Double {
    val p = parseIso(s) ?: return Double.NaN
    if ((s ?: "").length != 10) return Double.NaN
    val c = GregorianCalendar(p[0], p[1] - 1, p[2], 0, 0, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis.toDouble()
}

fun addM(s: String?, m: Int): Double {
    val p = parseIso(s) ?: return Double.NaN
    val c = GregorianCalendar(p[0], p[1] - 1, 1, 0, 0, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.isLenient = true
    c.add(Calendar.MONTH, m)
    c.add(Calendar.DAY_OF_MONTH, p[2] - 1)
    return c.timeInMillis.toDouble()
}

fun ymd(t: Double): String {
    val c = GregorianCalendar()
    c.timeInMillis = t.toLong()
    return "${c.get(Calendar.YEAR)}-${z2(c.get(Calendar.MONTH) + 1)}-${z2(c.get(Calendar.DAY_OF_MONTH))}"
}

fun fd(t: Double): String {
    if (t.isNaN()) return "NaN/NaN/NaN"
    val c = GregorianCalendar()
    c.timeInMillis = t.toLong()
    return "${z2(c.get(Calendar.DAY_OF_MONTH))}/${z2(c.get(Calendar.MONTH) + 1)}/${c.get(Calendar.YEAR)}"
}

fun dm(s: String?): String {
    val v = s ?: ""
    return if (ISO.containsMatchIn(v)) v.substring(8, 10) + "/" + v.substring(5, 7) + "/" + v.substring(0, 4) else v
}

fun stamp(t: Long = System.currentTimeMillis()): String {
    val c = GregorianCalendar()
    c.timeInMillis = t
    return "${z2(c.get(Calendar.DAY_OF_MONTH))}/${z2(c.get(Calendar.MONTH) + 1)}/${c.get(Calendar.YEAR)} " +
        "${z2(c.get(Calendar.HOUR_OF_DAY))}:${z2(c.get(Calendar.MINUTE))}"
}

fun today(): String = ymd(System.currentTimeMillis().toDouble())

fun ago(n: Int): String = ymd((System.currentTimeMillis() - n * 864e5).toDouble())

fun latinDigits(v: String): String = buildString {
    for (ch in v) {
        val i = AR_DIGITS.indexOf(ch)
        append(if (i >= 0) ('0' + i) else ch)
    }
}

fun pd(input: String?): String {
    val v = latinDigits((input ?: "").trim()).replace(Regex("""[.\-\s]"""), "/")
    Regex("""^(\d{1,2})/(\d{1,2})/(\d{2}|\d{4})$""").find(v)?.let { m ->
        val d = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        var y = m.groupValues[3].toInt()
        if (y < 100) y += 2000
        val c = GregorianCalendar(y, mo - 1, d)
        c.isLenient = true
        if (c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == mo - 1 && c.get(Calendar.DAY_OF_MONTH) == d) {
            return "$y-${z2(mo)}-${z2(d)}"
        }
    }
    Regex("""^(\d{4})/(\d{1,2})/(\d{1,2})$""").find(v)?.let { m ->
        return pd(m.groupValues[3] + "/" + m.groupValues[2] + "/" + m.groupValues[1])
    }
    return ""
}

fun dmask(raw: String): String {
    val d = raw.filter { it.isDigit() }.let(::latinDigits).take(8)
    var o = d.take(2)
    if (d.length > 2) o += "/" + d.substring(2, minOf(4, d.length))
    if (d.length > 4) o += "/" + d.substring(4)
    return o
}

fun NUM(v: Any?): Double {
    if (v is Number) return v.toDouble()
    val s = latinDigits((v ?: "").toString().trim()).replace(Regex("""[,٬\s]"""), "").replace("٫", ".")
    return if (s.isEmpty()) Double.NaN else s.toDoubleOrNull() ?: Double.NaN
}
