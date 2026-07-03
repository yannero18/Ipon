package com.ipon.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.ui.graphics.vector.ImageVector
import com.ipon.app.data.local.DailyReflectionEntity
import com.ipon.app.data.local.MoodRating

/**
 * Display metadata for each mood, kept here rather than scattered across
 * UI files so the emoji/label pairing has exactly one source of truth.
 */
fun MoodRating.emoji(): String = when (this) {
    MoodRating.GREAT -> "😄"
    MoodRating.OKAY -> "😐"
    MoodRating.STRESSED -> "😫"
    MoodRating.REGRET -> "😭"
}

fun MoodRating.icon(): ImageVector = when (this) {
    MoodRating.GREAT -> Icons.Outlined.SentimentVerySatisfied
    MoodRating.OKAY -> Icons.Outlined.SentimentNeutral
    MoodRating.STRESSED -> Icons.Outlined.SentimentDissatisfied
    MoodRating.REGRET -> Icons.Outlined.SentimentVeryDissatisfied
}

fun MoodRating.label(): String = when (this) {
    MoodRating.GREAT -> "Good about it"
    MoodRating.OKAY -> "It's fine"
    MoodRating.STRESSED -> "Stressed"
    MoodRating.REGRET -> "Regret some of it"
}

data class DailyReflection(
    val id: String,
    val dayKey: String,
    val mood: MoodRating,
    val note: String?,
    val createdAtEpochMillis: Long
)

fun DailyReflectionEntity.toDomain(): DailyReflection = DailyReflection(
    id = id,
    dayKey = dayKey,
    mood = mood,
    note = note,
    createdAtEpochMillis = createdAtEpochMillis
)
