package com.ipon.app.data.model

import com.ipon.app.data.local.EnvelopeEntity
import com.ipon.app.util.Money

/**
 * An envelope paired with how much has actually been spent against it this
 * period. [spent] comes from a live aggregate over the transactions table
 * (see TransactionRepository.observeCategoryBreakdownBetween), not a
 * cached/stored counter -- so it can never drift out of sync with the
 * ledger the way a manually-incremented counter could.
 */
data class EnvelopeProgress(
    val id: String,
    val category: ExpenseCategory,
    val periodYearMonth: String,
    val cap: Money,
    val spent: Money,
    val priority: Int = 2,
    val customIcon: String? = null
) {
    val remaining: Money get() = cap - spent
    val isOverBudget: Boolean get() = spent > cap

    /** 0f..1f+ (can exceed 1f when over budget -- UI is responsible for clamping the bar, not this model). */
    val fractionUsed: Float
        get() = if (cap.isZero) 0f else spent.minorUnits.toFloat() / cap.minorUnits.toFloat()

    /**
     * Pure pace-based projection: "at the rate you're spending, you'll hit
     * X by month's end." [dayOfMonth] and [daysInMonth] are passed in
     * (rather than read from Calendar.getInstance() internally) so this
     * stays a deterministic, testable function -- the caller decides what
     * "today" means.
     */
    fun projectedAtCurrentPace(dayOfMonth: Int, daysInMonth: Int): Money =
        projectSpend(spent, dayOfMonth, daysInMonth)
}

fun EnvelopeEntity.toDomainCategory(): ExpenseCategory = ExpenseCategory.fromDisplayName(category)
