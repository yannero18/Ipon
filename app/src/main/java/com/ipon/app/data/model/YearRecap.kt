package com.ipon.app.data.model

import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.local.TransactionType
import com.ipon.app.util.Money
import com.ipon.app.util.sum
import java.util.Calendar

/**
 * A yearly synthesis pulling together data that already exists across
 * Insights (transactions), Goals (contributions), and Reflection (mood
 * check-ins) into one summary screen. Nothing new is tracked or stored for
 * this -- it's a read-only view computed from the same tables everything
 * else in the app already uses.
 */
data class YearRecap(
    val year: Int,
    val totalIncome: Money,
    val totalExpense: Money,
    val transactionCount: Int,
    val topCategories: List<CategorySlice>,
    val busiestMonth: MonthActivity?,
    val moodCounts: Map<MoodRating, Int>,
    val goalsCompletedThisYear: Int,
    val totalSavedTowardGoals: Money
) {
    val netSaved: Money get() = totalIncome - totalExpense
    val totalReflections: Int get() = moodCounts.values.sum()
    val dominantMood: MoodRating? get() = moodCounts.maxByOrNull { it.value }?.key
}

/** A category and its total -- plain data, lives in the model layer (not data/repository) so other model-layer code, like YearRecap's aggregation, can reference it without depending on a repository class. */
data class CategorySlice(
    val category: String,
    val total: Money
)

data class MonthActivity(
    val monthIndex: Int,
    val transactionCount: Int
)

/**
 * Pure aggregation over already-fetched data -- no I/O here, so this is
 * trivially testable with plain lists. Repository layer is responsible for
 * fetching [transactions], [categoryBreakdown], [moodCounts] (already
 * bucketed), and [goalsCompletedThisYear]/[totalSavedTowardGoals] for the
 * year in question; this function just shapes them into one [YearRecap].
 */
fun buildYearRecap(
    year: Int,
    transactions: List<Transaction>,
    categoryBreakdown: List<CategorySlice>,
    moodCounts: Map<MoodRating, Int>,
    goalsCompletedThisYear: Int,
    totalSavedTowardGoals: Money
): YearRecap {
    val income = transactions
        .filter { it.type == TransactionType.INCOME }
        .map { it.amount }
        .sum()
    val expense = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .map { it.amount }
        .sum()

    val busiestMonth = transactions
        .groupBy { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.occurredAtEpochMillis
            cal.get(Calendar.MONTH)
        }
        .maxByOrNull { it.value.size }
        ?.let { (monthIndex, txs) -> MonthActivity(monthIndex, txs.size) }

    return YearRecap(
        year = year,
        totalIncome = income,
        totalExpense = expense,
        transactionCount = transactions.size,
        topCategories = categoryBreakdown.sortedByDescending { it.total.minorUnits }.take(3),
        busiestMonth = busiestMonth,
        moodCounts = moodCounts,
        goalsCompletedThisYear = goalsCompletedThisYear,
        totalSavedTowardGoals = totalSavedTowardGoals
    )
}
