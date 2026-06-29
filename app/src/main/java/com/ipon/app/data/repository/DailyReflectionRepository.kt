package com.ipon.app.data.repository

import com.ipon.app.data.local.DailyReflectionDao
import com.ipon.app.data.local.DailyReflectionEntity
import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.local.TransactionDao
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.DailyReflection
import com.ipon.app.data.model.toDomain
import com.ipon.app.util.Money
import com.ipon.app.util.sum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Pairs a day's mood check-in with what actually happened financially that
 * day -- this is the part that makes Reflection a genuine ritual tied to
 * real numbers rather than a generic standalone mood tracker. See
 * DailyReflectionEntity's header comment for the framing.
 */
data class DayInReview(
    val dayKey: String,
    val totalSpent: Money,
    val totalIncome: Money,
    val transactionCount: Int,
    val existingReflection: DailyReflection?
)

class DailyReflectionRepository(
    private val reflectionDao: DailyReflectionDao,
    private val transactionDao: TransactionDao
) {

    /**
     * Live view of "today so far" -- spend/income totals for [dayKey]'s
     * 24-hour window plus whatever reflection (if any) already exists for
     * that day, so re-opening the check-in later the same day shows what
     * you already chose instead of a blank slate.
     */
    fun observeDayInReview(
        dayKey: String,
        startOfDayEpochMillis: Long,
        endOfDayEpochMillis: Long
    ): Flow<DayInReview> =
        combine(
            transactionDao.observeBetween(startOfDayEpochMillis, endOfDayEpochMillis),
            reflectionDao.observeForDay(dayKey)
        ) { transactions, reflectionEntity ->
            val spent = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .map { Money.ofMinorUnits(it.amountMinorUnits) }
                .sum()
            val income = transactions
                .filter { it.type == TransactionType.INCOME }
                .map { Money.ofMinorUnits(it.amountMinorUnits) }
                .sum()

            DayInReview(
                dayKey = dayKey,
                totalSpent = spent,
                totalIncome = income,
                transactionCount = transactions.size,
                existingReflection = reflectionEntity?.toDomain()
            )
        }

    fun observeRecent(limit: Int = 14): Flow<List<DailyReflection>> =
        reflectionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun saveReflection(dayKey: String, mood: MoodRating, note: String?) {
        val existing = reflectionDao.getForDay(dayKey)
        reflectionDao.upsert(
            DailyReflectionEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                dayKey = dayKey,
                mood = mood,
                note = note
            )
        )
    }
}
