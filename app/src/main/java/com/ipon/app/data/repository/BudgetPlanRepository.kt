package com.ipon.app.data.repository

import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.FiftyThirtyTwentyComparison
import com.ipon.app.data.model.ZeroBasedBudget
import com.ipon.app.data.model.buildFiftyThirtyTwentyComparison
import com.ipon.app.util.Money
import com.ipon.app.util.sum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Both 50/30/20 and zero-based budgeting are reference frameworks held up
 * against data the app already tracks -- this repository doesn't store
 * anything new, it just combines existing repositories' Flows differently
 * than Insights or Envelopes do. Deliberately its own repository (not
 * bolted onto TransactionRepository) since, like RecapRepository, this is
 * a cross-cutting concern that doesn't belong to any single domain.
 */
class BudgetPlanRepository(
    private val transactionRepository: TransactionRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val recurringTemplateRepository: RecurringTemplateRepository
) {

    fun observeFiftyThirtyTwenty(
        monthStart: Long,
        monthEnd: Long
    ): Flow<FiftyThirtyTwentyComparison> =
        combine(
            transactionRepository.observeSummaryBetween(monthStart, monthEnd),
            transactionRepository.observeCategoryBreakdownBetween(monthStart, monthEnd)
        ) { summary, breakdown ->
            buildFiftyThirtyTwentyComparison(income = summary.income, categoryTotals = breakdown)
        }

    fun observeZeroBasedBudget(
        periodYearMonth: String,
        monthStart: Long,
        monthEnd: Long
    ): Flow<ZeroBasedBudget> =
        combine(
            transactionRepository.observeSummaryBetween(monthStart, monthEnd),
            envelopeRepository.observeProgressForPeriod(periodYearMonth, monthStart, monthEnd),
            recurringTemplateRepository.observeAll()
        ) { summary, envelopeProgress, recurringTemplates ->
            val totalCaps = envelopeProgress.map { it.cap }.sum()
            val totalRecurringExpense = recurringTemplates
                .filter { it.type == TransactionType.EXPENSE && !it.isPaused }
                .map { it.amount }
                .sum()

            ZeroBasedBudget(
                income = summary.income,
                totalEnvelopeCaps = totalCaps,
                totalRecurringExpensesThisMonth = totalRecurringExpense,
                // Goal contributions made THIS specific month aren't
                // separately queryable from GoalRepository.observeProgress()
                // (which returns lifetime totals, not month-scoped ones) --
                // rather than add a new month-scoped query just for this
                // one number, zero-based budgeting here treats "money
                // assigned to goals" as 0 for now and is explicit about
                // that gap in the UI, instead of silently showing a wrong
                // number derived from lifetime totals.
                totalGoalContributionsThisMonth = Money.ZERO
            )
        }
}
