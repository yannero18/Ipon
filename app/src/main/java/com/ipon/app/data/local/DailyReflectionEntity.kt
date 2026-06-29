package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * How the proposal's "anxiety-inducing chore -> mindful ritual" framing
 * actually gets delivered: a short, optional, end-of-day check-in tied to
 * that day's real numbers, not a generic standalone mood tracker bolted
 * onto a finance app.
 *
 * Deliberately minimal -- one tap for mood, an optional one-line note. No
 * required fields, no streak-shaming if skipped, no guilt mechanics. The
 * proposal's whole pitch is reducing financial anxiety; a reflection
 * feature that nags or judges would directly undercut that.
 *
 * [dayKey] is "YYYY-MM-DD", giving a natural one-reflection-per-day
 * uniqueness constraint without any date-range query math.
 */
enum class MoodRating { GREAT, OKAY, STRESSED, REGRET }

@Entity(tableName = "daily_reflections")
data class DailyReflectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /** Format: "YYYY-MM-DD". One reflection per day -- see the unique index in the schema SQL. */
    @ColumnInfo(name = "dayKey")
    val dayKey: String,

    @ColumnInfo(name = "mood")
    val mood: MoodRating,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "createdAtEpochMillis")
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
