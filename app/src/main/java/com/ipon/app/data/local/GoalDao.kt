package com.ipon.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class GoalSavedTotal(
    val goalId: String,
    val savedMinorUnits: Long
)

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY createdAtEpochMillis ASC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContribution(contribution: GoalContributionEntity)

    @Delete
    suspend fun deleteContribution(contribution: GoalContributionEntity)

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY contributedAtEpochMillis DESC")
    fun observeContributionsForGoal(goalId: String): Flow<List<GoalContributionEntity>>

    /**
     * Live saved total per goal, across ALL goals at once -- this powers the
     * Goals screen's list view, where every goal card needs its progress bar
     * without firing one query per card.
     */
    @Query(
        """
        SELECT goalId, COALESCE(SUM(amountMinorUnits), 0) AS savedMinorUnits
        FROM goal_contributions
        GROUP BY goalId
        """
    )
    fun observeSavedTotals(): Flow<List<GoalSavedTotal>>

    /**
     * Month-scoped saved totals per goal within a specific timestamp range.
     */
    @Query(
        """
        SELECT goalId, COALESCE(SUM(amountMinorUnits), 0) AS savedMinorUnits
        FROM goal_contributions
        WHERE contributedAtEpochMillis >= :startEpochMillis AND contributedAtEpochMillis <= :endEpochMillis
        GROUP BY goalId
        """
    )
    fun observeSavedTotalsBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<GoalSavedTotal>>

    @Query("SELECT * FROM goal_contributions ORDER BY contributedAtEpochMillis DESC")
    fun observeAllContributions(): Flow<List<GoalContributionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinorUnits), 0)
        FROM goal_contributions
        WHERE contributedAtEpochMillis >= :startEpochMillis AND contributedAtEpochMillis <= :endEpochMillis
        """
    )
    fun observeContributionsSumBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<Long>

    @Query("SELECT * FROM goals ORDER BY createdAtEpochMillis ASC")
    suspend fun getAllGoalsEver(): List<GoalEntity>
}