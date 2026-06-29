package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(envelope: EnvelopeEntity)

    @Update
    suspend fun update(envelope: EnvelopeEntity)

    @Delete
    suspend fun delete(envelope: EnvelopeEntity)

    @Query("SELECT * FROM envelopes WHERE periodYearMonth = :periodYearMonth ORDER BY category ASC")
    fun observeForPeriod(periodYearMonth: String): Flow<List<EnvelopeEntity>>

    /** Every envelope ever set, across all months -- used only by export, where historical caps are genuinely part of "all your data," not just the current month's. */
    @Query("SELECT * FROM envelopes ORDER BY periodYearMonth DESC, category ASC")
    suspend fun getAllEver(): List<EnvelopeEntity>

    @Query("SELECT * FROM envelopes WHERE periodYearMonth = :periodYearMonth AND category = :category LIMIT 1")
    suspend fun getForCategoryAndPeriod(periodYearMonth: String, category: String): EnvelopeEntity?

    @Query("SELECT * FROM envelopes WHERE periodYearMonth = :periodYearMonth")
    suspend fun getAllForPeriodOnce(periodYearMonth: String): List<EnvelopeEntity>
}
