package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val totalMinorUnits: Long
)

data class PeriodTotals(
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Suppress("RoomReturnValueTransfer")
    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE occurredAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        ORDER BY occurredAtEpochMillis DESC
        """
    )
    fun observeBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountMinorUnits ELSE 0 END), 0) AS incomeMinorUnits,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinorUnits ELSE 0 END), 0) AS expenseMinorUnits
        FROM transactions
        WHERE occurredAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        """
    )
    fun observeTotalsBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<PeriodTotals>

    @Query(
        """
        SELECT category, SUM(amountMinorUnits) AS totalMinorUnits
        FROM transactions
        WHERE type = 'EXPENSE' AND occurredAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
        GROUP BY category
        ORDER BY totalMinorUnits DESC
        """
    )
    fun observeCategoryBreakdownBetween(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?
}
