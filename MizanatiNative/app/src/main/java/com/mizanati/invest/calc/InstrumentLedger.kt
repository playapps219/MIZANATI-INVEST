package com.mizanati.invest.calc

import com.mizanati.invest.data.Instrument
import kotlin.math.*

object InstrumentLedger {
    data class Result(
        val cost: Double,
        val value: Double,
        val realized: Double,
        val units: Double,
        val flows: List<Pair<Long, Double>>,
        val age: Int,
        val coupon: Double = 0.0,
        val rent: Double = 0.0,
        val rentTotal: Double = 0.0,
    )

    fun calculate(i: Instrument, now: Long = System.currentTimeMillis()): Result {
        val flows = mutableListOf<Pair<Long, Double>>()
        val start = Finance.parseDate(i.start.ifBlank { i.buyD })
        val principal = i.price0
        if (i.kind == "cert" && principal > 0 && start != null && i.termM > 0) {
            val period = i.pmtM.takeIf { it > 0 } ?: i.termM
            val payment = principal * i.rate / 100 * period / 12 * (1 - i.feePct / 100)
            val maturity = Finance.parseDate(Finance.addMonths(i.start, i.termM)) ?: start
            val count = max(1, i.termM / period).coerceAtMost(1200)
            flows.add(start to -principal)
            var received = 0
            var previous: Long = start
            var next: Long = maturity
            for (index in 1..count) {
                val date = Finance.parseDate(Finance.addMonths(i.start, index * period)) ?: continue
                if (date <= now) { received++; previous = date; flows.add(date to payment) }
                else { next = date; break }
            }
            val matured = maturity <= now
            if (matured) flows.add(maturity to principal)
            val accrued = if (matured || next <= previous) 0.0 else payment * ((now - previous).toDouble() / (next - previous)).coerceIn(0.0, 1.0)
            return Result(if (matured) 0.0 else principal, if (matured) 0.0 else principal + accrued,
                received * payment, 0.0, flows, max(0, ((now - start) / 86400000).toInt()), received * payment)
        }
        if (i.kind == "prop" && principal > 0 && start != null) {
            val base = principal * (1 + i.feePct / 100)
            flows.add(start to -base)
            var income = 0.0
            var expenses = 0.0
            for (index in 1..1200) {
                val date = Finance.parseDate(Finance.addMonths(i.rentStart.ifBlank { i.start }, index)) ?: break
                if (date > now) break
                flows.add(date to i.rent); income += i.rent
            }
            for (index in 1..1200) {
                val date = Finance.parseDate(Finance.addMonths(i.start, index)) ?: break
                if (date > now) break
                flows.add(date to -i.expM); expenses += i.expM
            }
            return Result(base, if (i.cvDate.isNotBlank()) i.cv else base, income - expenses, 0.0,
                flows, max(0, ((now - start) / 86400000).toInt()), rent = i.rent - i.expM, rentTotal = income - expenses)
        }
        var units = 0.0
        var unitCost = 0.0
        var cashCost = 0.0
        var realized = 0.0
        var weightedAge = 0.0
        var purchases = 0.0
        var adjustment = 0.0
        i.tx.sortedBy { it.d }.forEach { tx ->
            val date = Finance.parseDate(tx.d) ?: return@forEach
            val amount = abs(tx.amt)
            when (tx.typ) {
                "buy" -> {
                    purchases += amount
                    weightedAge += amount * max(0.0, (now - date).toDouble() / 86400000)
                    if (tx.units > 0) { units += tx.units; unitCost += amount }
                    else { cashCost += amount; if (tx.d > i.cvDate) adjustment += amount }
                    flows.add(date to -amount)
                }
                "sell", "maturity" -> {
                    if (tx.units > 0) {
                        if (tx.units > units + 1e-9 || units <= 0) return@forEach
                        val released = unitCost * tx.units / units
                        unitCost -= released; units -= tx.units; realized += amount - released
                    } else {
                        val base = if (i.cv > 0) i.cv + amount else cashCost
                        val released = cashCost * if (base > 0) min(1.0, amount / base) else 0.0
                        cashCost -= released; realized += amount - released
                        if (tx.d > i.cvDate) adjustment -= amount
                    }
                    flows.add(date to amount)
                }
                "coupon", "rent" -> { realized += amount; flows.add(date to amount) }
                "fee" -> { realized -= amount; flows.add(date to -amount) }
            }
        }
        if (units < 1e-9) { units = 0.0; unitCost = 0.0 }
        val unitValue = if (units > 0) {
            if (i.mode == "v" && i.cvDate.isNotBlank()) i.cv else units * i.price
        } else 0.0
        val cashValue = if (cashCost > 0) {
            if (i.cvDate.isNotBlank() && units == 0.0) max(0.0, i.cv + adjustment) else cashCost
        } else if (units == 0.0 && i.cvDate.isNotBlank()) max(0.0, i.cv) else 0.0
        return Result(unitCost + cashCost, unitValue + cashValue, realized, units, flows,
            if (purchases > 0) (weightedAge / purchases).toInt() else 0)
    }
}
