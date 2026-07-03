package com.ipon.app.data.repository

import com.ipon.app.data.local.TransactionDao
import com.ipon.app.data.model.CategorySlice
import com.ipon.app.data.model.CategoryTrend
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.toDomain
import com.ipon.app.data.model.toEntity
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class PeriodSummary(
    val income: Money,
    val expense: Money
) {
    val net: Money get() = income - expense
}

class TransactionRepository(private val dao: TransactionDao) {

    fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<Transaction>> =
        dao.observeBetween(startEpochMillis, endEpochMillis).map { list -> list.map { it.toDomain() } }

    fun observeSummaryBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<PeriodSummary> =
        dao.observeTotalsBetween(startEpochMillis, endEpochMillis).map { totals ->
            PeriodSummary(
                income = Money.ofMinorUnits(totals.incomeMinorUnits),
                expense = Money.ofMinorUnits(totals.expenseMinorUnits)
            )
        }

    fun observeCategoryBreakdownBetween(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Flow<List<CategorySlice>> =
        dao.observeCategoryBreakdownBetween(startEpochMillis, endEpochMillis).map { rows ->
            rows.map { CategorySlice(it.category, Money.ofMinorUnits(it.totalMinorUnits)) }
        }

    /**
     * Combines this-month and last-month category breakdowns into trend
     * objects -- built from two calls to the same aggregate query already
     * used by Insights, not a new statistic computed and stored anywhere.
     * Categories present in only one of the two periods still appear, with
     * zero standing in for the period they have no spend in (e.g. a
     * category you started using this month for the first time shows
     * "up" against a real zero, not a missing/null comparison).
     */
    fun observeCategoryTrends(
        thisMonthStart: Long,
        thisMonthEnd: Long,
        lastMonthStart: Long,
        lastMonthEnd: Long
    ): Flow<List<CategoryTrend>> =
        combine(
            observeCategoryBreakdownBetween(thisMonthStart, thisMonthEnd),
            observeCategoryBreakdownBetween(lastMonthStart, lastMonthEnd)
        ) { thisMonth, lastMonth ->
            val thisMonthByCategory = thisMonth.associate { it.category to it.total }
            val lastMonthByCategory = lastMonth.associate { it.category to it.total }
            val allCategoryNames = thisMonthByCategory.keys + lastMonthByCategory.keys

            allCategoryNames.map { categoryName ->
                CategoryTrend(
                    category = ExpenseCategory.fromDisplayName(categoryName),
                    thisMonth = thisMonthByCategory[categoryName] ?: Money.ZERO,
                    lastMonth = lastMonthByCategory[categoryName] ?: Money.ZERO
                )
            }
        }

    /**
     * Average spend per category over [monthCount] complete prior months
     * (not including the current, still-in-progress month) -- used to
     * suggest a sensible envelope cap from real history rather than asking
     * the person to guess a number from nothing. A simple mean, not a
     * weighted or seasonally-adjusted one; this is meant to be a starting
     * point the person can edit, not a precise forecast.
     */
    fun observeCategoryAverages(monthCount: Int, now: Calendar = Calendar.getInstance()): Flow<List<CategorySlice>> {
        val ranges = (1..monthCount).map { offset ->
            val cal = now.clone() as Calendar
            cal.add(Calendar.MONTH, -offset)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            start to cal.timeInMillis
        }

        val flows = ranges.map { (start, end) -> observeCategoryBreakdownBetween(start, end) }
        return combine(flows) { monthlyBreakdowns ->
            val totalsByCategory = mutableMapOf<String, Long>()
            monthlyBreakdowns.forEach { month ->
                month.forEach { slice ->
                    totalsByCategory[slice.category] = (totalsByCategory[slice.category] ?: 0L) + slice.total.minorUnits
                }
            }
            totalsByCategory.map { (category, total) ->
                CategorySlice(category, Money.ofMinorUnits(total / monthCount))
            }.sortedByDescending { it.total.minorUnits }
        }
    }

    suspend fun add(transaction: Transaction) = dao.insert(transaction.toEntity())

    suspend fun update(transaction: Transaction) = dao.update(transaction.toEntity())

    suspend fun delete(transaction: Transaction) = dao.delete(transaction.toEntity())

    suspend fun getById(id: String): Transaction? = dao.getById(id)?.toDomain()
}
