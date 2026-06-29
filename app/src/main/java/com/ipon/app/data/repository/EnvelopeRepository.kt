package com.ipon.app.data.repository

import com.ipon.app.data.local.EnvelopeDao
import com.ipon.app.data.local.EnvelopeEntity
import com.ipon.app.data.local.TransactionDao
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class EnvelopeRepository(
    private val envelopeDao: EnvelopeDao,
    private val transactionDao: TransactionDao
) {

    /**
     * Live envelope progress for a given month, keyed by the month's
     * [startEpochMillis]/[endEpochMillis] range for the spend aggregate and
     * [periodYearMonth] ("YYYY-MM") for the stored caps.
     *
     * Caps with zero spend this period still show up here (spent = 0), since
     * the source list is the envelopes themselves, not the categories that
     * happen to have transactions -- an envelope you haven't touched yet
     * should still appear, sitting at 0%, not silently disappear.
     */
    fun observeProgressForPeriod(
        periodYearMonth: String,
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Flow<List<EnvelopeProgress>> =
        combine(
            envelopeDao.observeForPeriod(periodYearMonth),
            transactionDao.observeCategoryBreakdownBetween(startEpochMillis, endEpochMillis)
        ) { envelopes, categoryTotals ->
            val spentByCategory = categoryTotals.associate { it.category to it.totalMinorUnits }
            envelopes.map { envelope ->
                EnvelopeProgress(
                    id = envelope.id,
                    category = ExpenseCategory.fromDisplayName(envelope.category),
                    periodYearMonth = envelope.periodYearMonth,
                    cap = Money.ofMinorUnits(envelope.capMinorUnits),
                    spent = Money.ofMinorUnits(spentByCategory[envelope.category] ?: 0L)
                )
            }
        }

    suspend fun setCap(category: ExpenseCategory, periodYearMonth: String, cap: Money) {
        val existing = envelopeDao.getForCategoryAndPeriod(periodYearMonth, category.displayName)
        val entity = EnvelopeEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            category = category.displayName,
            periodYearMonth = periodYearMonth,
            capMinorUnits = cap.minorUnits
        )
        envelopeDao.upsert(entity)
    }

    suspend fun removeCap(category: ExpenseCategory, periodYearMonth: String) {
        envelopeDao.getForCategoryAndPeriod(periodYearMonth, category.displayName)?.let {
            envelopeDao.delete(it)
        }
    }

    /**
     * Copies last month's envelope caps forward into the current month, for
     * categories that don't already have a cap set this month. Skips
     * categories already configured this month rather than overwriting, so
     * re-running this (e.g. opening the screen twice) is always safe.
     */
    suspend fun copyForwardFrom(previousPeriodYearMonth: String, targetPeriodYearMonth: String) {
        val previous = envelopeDao.getAllForPeriodOnce(previousPeriodYearMonth)
        val existingTargets = envelopeDao.getAllForPeriodOnce(targetPeriodYearMonth)
            .map { it.category }
            .toSet()

        previous
            .filter { it.category !in existingTargets }
            .forEach { old ->
                envelopeDao.upsert(
                    EnvelopeEntity(
                        category = old.category,
                        periodYearMonth = targetPeriodYearMonth,
                        capMinorUnits = old.capMinorUnits
                    )
                )
            }
    }
}
