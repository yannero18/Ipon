package com.ipon.app.data.repository

import com.ipon.app.data.local.TransactionDao
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.toDomain
import com.ipon.app.data.model.toEntity
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PeriodSummary(
    val income: Money,
    val expense: Money
) {
    val net: Money get() = income - expense
}

data class CategorySlice(
    val category: String,
    val total: Money
)

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

    suspend fun add(transaction: Transaction) = dao.insert(transaction.toEntity())

    suspend fun update(transaction: Transaction) = dao.update(transaction.toEntity())

    suspend fun delete(transaction: Transaction) = dao.delete(transaction.toEntity())
}
