package com.ipon.app.data.model

import com.ipon.app.util.Money
import com.ipon.app.util.sum
import java.math.BigDecimal

/**
 * Which 50/30/20 bucket a category falls into. This mapping is a judgment
 * call -- "needs" vs "wants" isn't objectively determined by category
 * alone (someone's daily coffee might be a need for them) -- so it's kept
 * as one explicit, visible table rather than buried logic, and the UI
 * shows the mapping plainly so the person can see exactly how their
 * categories were bucketed rather than trusting an opaque classification.
 */
enum class BudgetBucket { NEEDS, WANTS, SAVINGS_AND_DEBT }

fun ExpenseCategory.fiftyThirtyTwentyBucket(): BudgetBucket = when (this) {
    ExpenseCategory.BILLS,
    ExpenseCategory.GROCERIES,
    ExpenseCategory.HEALTH,
    ExpenseCategory.TRANSPO,
    ExpenseCategory.GOVERNMENT,
    ExpenseCategory.EDUCATION -> BudgetBucket.NEEDS

    ExpenseCategory.FOOD,
    ExpenseCategory.SHOPPING,
    ExpenseCategory.ENTERTAINMENT,
    ExpenseCategory.OTHER -> BudgetBucket.WANTS

    ExpenseCategory.UTANG,
    ExpenseCategory.PADALA -> BudgetBucket.SAVINGS_AND_DEBT
}

/**
 * Comparison of the classic 50/30/20 framework (50% needs, 30% wants, 20%
 * savings/debt) against the person's actual income and spend this month.
 * Pure math -- target amounts are simple percentages of [income], actual
 * amounts are real category totals bucketed via [fiftyThirtyTwentyBucket].
 * This is a reference framework being held up against reality, not a
 * forecast or a rule the app enforces.
 */
data class FiftyThirtyTwentyComparison(
    val income: Money,
    val needsTarget: Money,
    val needsActual: Money,
    val wantsTarget: Money,
    val wantsActual: Money,
    val savingsTarget: Money,
    val savingsActual: Money
) {
    val needsOverTarget: Boolean get() = needsActual > needsTarget
    val wantsOverTarget: Boolean get() = wantsActual > wantsTarget
    val savingsUnderTarget: Boolean get() = savingsActual < savingsTarget
}

fun buildFiftyThirtyTwentyComparison(income: Money, categoryTotals: List<CategorySlice>): FiftyThirtyTwentyComparison {
    val byBucket = categoryTotals
        .groupBy { ExpenseCategory.fromDisplayName(it.category).fiftyThirtyTwentyBucket() }
        .mapValues { (_, slices) -> slices.map { it.total }.sum() }

    val needsActual = byBucket[BudgetBucket.NEEDS] ?: Money.ZERO
    val wantsActual = byBucket[BudgetBucket.WANTS] ?: Money.ZERO
    val savingsDebtActual = byBucket[BudgetBucket.SAVINGS_AND_DEBT] ?: Money.ZERO

    return FiftyThirtyTwentyComparison(
        income = income,
        needsTarget = income.scaledBy(BigDecimal("0.50")),
        needsActual = needsActual,
        wantsTarget = income.scaledBy(BigDecimal("0.30")),
        wantsActual = wantsActual,
        savingsTarget = income.scaledBy(BigDecimal("0.20")),
        // "Savings" actual is framed as income minus everything spent --
        // not just the UTANG/PADALA bucket -- since that's what's actually
        // left over to save, which is the number 50/30/20 cares about.
        savingsActual = income - needsActual - wantsActual - savingsDebtActual
    )
}

/**
 * Zero-based budgeting view: "every peso has a job." Shows income against
 * everything it's already been assigned to (envelope caps, this month's
 * recurring templates, goal contributions made this month) and what's left
 * unassigned. Like 50/30/20, this is pure arithmetic over data the app
 * already has -- envelope caps, recurring amounts, goal contributions --
 * not a new tracked concept.
 */
data class ZeroBasedBudget(
    val income: Money,
    val totalEnvelopeCaps: Money,
    val totalRecurringExpensesThisMonth: Money,
    val totalGoalContributionsThisMonth: Money
) {
    val totalAssigned: Money get() = totalEnvelopeCaps + totalRecurringExpensesThisMonth + totalGoalContributionsThisMonth
    val unassigned: Money get() = income - totalAssigned
    val isFullyAssigned: Boolean get() = unassigned.isZero
    val isOverAssigned: Boolean get() = unassigned.isNegative
}
