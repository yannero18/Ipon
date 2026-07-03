package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY periodYearMonth DESC, createdAtEpochMillis DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE periodYearMonth = :periodYearMonth LIMIT 1")
    suspend fun getReportForPeriod(periodYearMonth: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: String)
}
