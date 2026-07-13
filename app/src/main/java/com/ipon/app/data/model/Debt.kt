package com.ipon.app.data.model

import com.ipon.app.data.local.DebtEntity
import com.ipon.app.util.Money

data class Debt(
    val id: String,
    val label: String,
    val originalBalance: Money,
    val interestRatePercent: Double?,
    /** Upfront fee deducted before the cash reaches you -- 0 if this loan had no such fee. */
    val fee: Money = Money.ZERO,
    /** Free-text single deadline, e.g. "Jul 25, 2026" -- null if this debt has no fixed due date. */
    val dueDate: String? = null,
    val isArchived: Boolean
) {
    /** What you actually received in hand -- originalBalance minus the upfront fee. Equal to originalBalance when fee is zero. */
    val netProceedsReceived: Money get() = originalBalance - fee
}

data class DebtProgress(
    val debt: Debt,
    val paid: Money
) {
    val remaining: Money get() = debt.originalBalance - paid
    val isPaidOff: Boolean get() = paid >= debt.originalBalance

    val fractionPaid: Float
        get() = if (debt.originalBalance.isZero) 0f else paid.minorUnits.toFloat() / debt.originalBalance.minorUnits.toFloat()
}

enum class PayoffMethod {
    /** Smallest remaining balance first -- prioritizes quick wins and momentum over interest math. */
    SNOWBALL,
    /** Highest interest rate first -- minimizes total interest paid over time, the mathematically optimal order. */
    AVALANCHE
}

/**
 * Orders active (not-yet-paid-off) debts by the chosen payoff method.
 * Pure sorting logic -- this doesn't simulate payment schedules or compute
 * payoff dates, it just answers "which debt should extra money go to
 * first," which is the actual decision snowball/avalanche methods exist
 * to make.
 */
fun List<DebtProgress>.orderedForPayoff(method: PayoffMethod): List<DebtProgress> {
    val active = filter { !it.isPaidOff }
    return when (method) {
        PayoffMethod.SNOWBALL -> active.sortedBy { it.remaining.minorUnits }
        PayoffMethod.AVALANCHE -> active.sortedWith(
            compareByDescending<DebtProgress> { it.debt.interestRatePercent ?: -1.0 }
                .thenBy { it.remaining.minorUnits }
        )
    }
}

fun DebtEntity.toDomain(): Debt = Debt(
    id = id,
    label = label,
    originalBalance = Money.ofMinorUnits(originalBalanceMinorUnits),
    interestRatePercent = interestRatePercent,
    fee = Money.ofMinorUnits(feeMinorUnits),
    dueDate = dueDate,
    isArchived = isArchived
)

fun Debt.toEntity(): DebtEntity = DebtEntity(
    id = id,
    label = label,
    originalBalanceMinorUnits = originalBalance.minorUnits,
    interestRatePercent = interestRatePercent,
    feeMinorUnits = fee.minorUnits,
    dueDate = dueDate,
    isArchived = isArchived
)
