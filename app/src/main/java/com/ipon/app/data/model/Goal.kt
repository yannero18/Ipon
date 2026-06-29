package com.ipon.app.data.model

import com.ipon.app.data.local.GoalEntity
import com.ipon.app.util.Money

data class Goal(
    val id: String,
    val label: String,
    val target: Money,
    val emoji: String,
    val isArchived: Boolean
)

data class GoalProgress(
    val goal: Goal,
    val saved: Money
) {
    val remaining: Money get() = goal.target - saved
    val isComplete: Boolean get() = saved >= goal.target

    /** 0f..1f+, never negative; UI clamps for the progress bar fill. */
    val fraction: Float
        get() = if (goal.target.isZero) 0f else saved.minorUnits.toFloat() / goal.target.minorUnits.toFloat()
}

data class GoalContribution(
    val id: String,
    val goalId: String,
    val amount: Money,
    val note: String?,
    val contributedAtEpochMillis: Long
)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    label = label,
    target = Money.ofMinorUnits(targetMinorUnits),
    emoji = emoji,
    isArchived = isArchived
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    label = label,
    targetMinorUnits = target.minorUnits,
    emoji = emoji,
    isArchived = isArchived
)
