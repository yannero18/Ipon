package com.ipon.app.data.repository

import com.ipon.app.data.local.GoalContributionEntity
import com.ipon.app.data.local.GoalDao
import com.ipon.app.data.local.GoalEntity
import com.ipon.app.data.model.Goal
import com.ipon.app.data.model.GoalContribution
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.data.model.toDomain
import com.ipon.app.data.model.toEntity
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class GoalRepository(private val dao: GoalDao) {

    fun observeProgress(): Flow<List<GoalProgress>> =
        combine(dao.observeActive(), dao.observeSavedTotals()) { goals, savedTotals ->
            val savedByGoalId = savedTotals.associate { it.goalId to it.savedMinorUnits }
            goals.map { goal ->
                GoalProgress(
                    goal = goal.toDomain(),
                    saved = Money.ofMinorUnits(savedByGoalId[goal.id] ?: 0L)
                )
            }
        }

    fun observeProgressForMonth(startEpochMillis: Long, endEpochMillis: Long): Flow<List<GoalProgress>> =
        combine(dao.observeActive(), dao.observeSavedTotalsBetween(startEpochMillis, endEpochMillis)) { goals, savedTotals ->
            val savedByGoalId = savedTotals.associate { it.goalId to it.savedMinorUnits }
            goals.map { goal ->
                GoalProgress(
                    goal = goal.toDomain(),
                    saved = Money.ofMinorUnits(savedByGoalId[goal.id] ?: 0L)
                )
            }
        }

    fun observeContributionsForGoal(goalId: String): Flow<List<GoalContribution>> =
        dao.observeContributionsForGoal(goalId).map { list ->
            list.map {
                GoalContribution(
                    id = it.id,
                    goalId = it.goalId,
                    amount = Money.ofMinorUnits(it.amountMinorUnits),
                    note = it.note,
                    contributedAtEpochMillis = it.contributedAtEpochMillis
                )
            }
        }

    // UPDATED to accept imageUri
    suspend fun createGoal(label: String, target: Money, emoji: String, deadline: String? = null, imageUri: String? = null) {
        dao.insert(
            GoalEntity(
                id = UUID.randomUUID().toString(),
                label = label,
                targetMinorUnits = target.minorUnits,
                emoji = emoji,
                deadline = deadline,
                imageUri = imageUri
            )
        )
    }

    suspend fun archiveGoal(goal: Goal) {
        dao.update(goal.copy(isArchived = true).toEntity())
    }

    suspend fun deleteGoal(goal: Goal) {
        dao.deleteGoalAndContributions(goal.toEntity())
    }

    suspend fun updateGoal(goal: Goal) {
        dao.update(goal.toEntity())
    }

    suspend fun addContribution(goalId: String, amount: Money, note: String?) {
        dao.insertContribution(
            GoalContributionEntity(
                goalId = goalId,
                amountMinorUnits = amount.minorUnits,
                note = note
            )
        )
    }

    fun observeAllContributions(): Flow<List<GoalContribution>> =
        dao.observeAllContributions().map { list ->
            list.map {
                GoalContribution(
                    id = it.id,
                    goalId = it.goalId,
                    amount = Money.ofMinorUnits(it.amountMinorUnits),
                    note = it.note,
                    contributedAtEpochMillis = it.contributedAtEpochMillis
                )
            }
        }

    fun observeContributionsSumBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<Money> =
        dao.observeContributionsSumBetween(startEpochMillis, endEpochMillis).map { Money.ofMinorUnits(it) }
}