package com.ipon.app.data.repository

import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.model.YearRecap
import com.ipon.app.data.model.buildYearRecap
import com.ipon.app.util.Money
import com.ipon.app.util.sum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

/**
 * Pulls together data that already exists across three other repositories
 * (transactions, goal contributions, daily reflections) into one yearly
 * synthesis. Deliberately its own repository rather than a method bolted
 * onto TransactionRepository or GoalRepository -- this aggregation doesn't
 * conceptually belong to any single one of them, it sits across all three.
 */
class RecapRepository(
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val dailyReflectionRepository: DailyReflectionRepository
) {

    fun observeRecapForYear(year: Int): Flow<YearRecap> {
        val (yearStart, yearEnd) = yearRangeMillis(year)

        return combine(
            transactionRepository.observeBetween(yearStart, yearEnd),
            transactionRepository.observeCategoryBreakdownBetween(yearStart, yearEnd),
            goalRepository.observeProgress(),
            // observeRecent takes a count, not a date range -- a generous
            // limit (400, comfortably more than 366 days) is fetched and
            // then filtered down to this specific year client-side, since
            // there's no "all reflections in a date range" query and one
            // boolean year filter isn't worth adding a new DAO method for.
            dailyReflectionRepository.observeRecent(limit = 400)
        ) { transactions, categoryBreakdown, goalProgress, reflections ->
            val reflectionsThisYear = reflections.filter { it.dayKey.startsWith(year.toString()) }
            val moodCounts: Map<MoodRating, Int> = reflectionsThisYear
                .groupingBy { it.mood }
                .eachCount()

            val goalsCompletedThisYear = goalProgress.count { it.isComplete }
            val totalSavedTowardGoals: Money = goalProgress.map { it.saved }.sum()

            buildYearRecap(
                year = year,
                transactions = transactions,
                categoryBreakdown = categoryBreakdown,
                moodCounts = moodCounts,
                goalsCompletedThisYear = goalsCompletedThisYear,
                totalSavedTowardGoals = totalSavedTowardGoals
            )
        }
    }

    private fun yearRangeMillis(year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return start to end
    }
}
