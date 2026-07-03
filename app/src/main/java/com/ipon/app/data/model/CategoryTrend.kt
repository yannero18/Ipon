package com.ipon.app.data.model

import com.ipon.app.util.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A category's spend this month compared to last month. Built entirely
 * from real transaction sums already available via
 * TransactionRepository.observeCategoryBreakdownBetween -- no separate
 * "trends" table or cached statistic, so this can never drift from the
 * actual ledger the way a precomputed/cached insight could.
 */
data class CategoryTrend(
    val category: ExpenseCategory,
    val thisMonth: Money,
    val lastMonth: Money
) {
    /** Positive = spent more this month. Null when last month was zero (no meaningful percentage). */
    val percentChange: Int?
        get() {
            if (lastMonth.isZero) return null
            return BigDecimal(thisMonth.minorUnits - lastMonth.minorUnits)
                .divide(BigDecimal(lastMonth.minorUnits), 4, RoundingMode.HALF_EVEN)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_EVEN)
                .toInt()
        }

    val direction: TrendDirection
        get() = when {
            thisMonth.minorUnits == lastMonth.minorUnits -> TrendDirection.FLAT
            thisMonth > lastMonth -> TrendDirection.UP
            else -> TrendDirection.DOWN
        }
}

enum class TrendDirection { UP, DOWN, FLAT }

/**
 * Pace-based projection for an envelope: "at this rate, you'll spend X by
 * month's end." Pure arithmetic over what has already happened this
 * month -- (spent so far / days elapsed) * days in month -- not a model or
 * forecast in any statistical sense, just an honest extrapolation of the
 * current rate. Framed that way in the UI too, so it doesn't overclaim
 * confidence it doesn't have.
 */
data class SpendPaceProjection(
    val category: ExpenseCategory,
    val spentSoFar: Money,
    val projectedTotal: Money,
    val cap: Money
) {
    val isProjectedToExceed: Boolean get() = projectedTotal > cap
}

fun projectSpend(spentSoFar: Money, dayOfMonth: Int, daysInMonth: Int): Money {
    if (dayOfMonth <= 0) return spentSoFar
    val projected = BigDecimal(spentSoFar.minorUnits)
        .multiply(BigDecimal(daysInMonth))
        .divide(BigDecimal(dayOfMonth), 0, RoundingMode.HALF_EVEN)
    return Money.ofMinorUnits(projected.toLong())
}

/**
 * "At this rate, your money runs out by the Nth" -- applies the same
 * pace-extrapolation idea as envelope pace projection, but to the whole
 * month's balance instead of one category. [currentBalance] is whatever
 * is left of this month's income after this month's spend so far;
 * [averageDailySpend] is that same month's total expense divided by days
 * elapsed. If spend continues at the same daily rate, this estimates which
 * day of the month the balance would hit zero. Returns null when spend is
 * effectively zero (nothing to extrapolate, and dividing by near-zero would
 * produce a meaningfully large "runway").
 */
fun estimateDaysOfRunway(currentBalance: Money, averageDailySpend: Money): Int? {
    if (averageDailySpend.minorUnits <= 0L) return null
    if (currentBalance.minorUnits <= 0L) return 0
    return (currentBalance.minorUnits / averageDailySpend.minorUnits).toInt()
}
