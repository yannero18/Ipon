package com.ipon.app.data.model

import com.ipon.app.data.local.DailyReflectionEntity
import com.ipon.app.data.local.MoodRating

/**
 * Display metadata for each mood, kept here rather than scattered across
 * UI files so the emoji/label pairing has exactly one source of truth.
 */
fun MoodRating.emoji(): String = when (this) {
    MoodRating.GREAT -> "\ud83d\ude0a"
    MoodRating.OKAY -> "\ud83d\ude10"
    MoodRating.STRESSED -> "\ud83d\ude15"
    MoodRating.REGRET -> "\ud83d\ude14"
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
