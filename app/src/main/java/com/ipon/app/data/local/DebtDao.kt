package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DebtPaidTotal(
    val debtId: String,
    val paidMinorUnits: Long
)

@Dao
interface DebtDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(debt: DebtEntity)

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)

    @Query("SELECT * FROM debts WHERE isArchived = 0 ORDER BY createdAtEpochMillis ASC")
    fun observeActive(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: DebtPaymentEntity)

    /** Live paid total per debt, across all debts at once -- mirrors GoalDao.observeSavedTotals(). */
    @Query(
        """
        SELECT debtId, COALESCE(SUM(amountMinorUnits), 0) AS paidMinorUnits
        FROM debt_payments
        GROUP BY debtId
        """
    )
    fun observePaidTotals(): Flow<List<DebtPaidTotal>>

    @Query("SELECT * FROM debts ORDER BY createdAtEpochMillis ASC")
    suspend fun getAllDebtsEver(): List<DebtEntity>

    @Query("SELECT * FROM debt_payments ORDER BY paidAtEpochMillis ASC")
    suspend fun getAllDebtPaymentsEver(): List<DebtPaymentEntity>
}