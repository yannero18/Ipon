package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A savings goal -- "Emergency fund," "Motorcycle," "Wedding" -- distinct
 * from Envelopes in both intent and mechanics. An Envelope tracks "don't
 * overspend X this month," derived passively from existing expense
 * transactions. A Goal tracks "I'm saving toward Y," and its progress comes
 * from deliberate contributions the user logs (see [GoalContributionEntity]),
 * not from anything inferred off the ledger.
 *
 * That's why goals get their own contribution log rather than reusing the
 * transactions table -- a goal contribution isn't income or an expense, and
 * forcing it into that table would corrupt the monthly in/out totals on the
 * Ledger and Insights screens.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "label")
    val label: String,

    /** Stored in minor units (centavos), same convention as everywhere else. */
    @ColumnInfo(name = "targetMinorUnits")
    val targetMinorUnits: Long,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "deadline")
    val deadline: String? = null,

    @ColumnInfo(name = "createdAtEpochMillis")
    val createdAtEpochMillis: Long = System.currentTimeMillis(),

    // NEW COLUMN FOR GOTYME-STYLE CUSTOM PHOTOS
    @ColumnInfo(name = "imageUri")
    val imageUri: String? = null
)

/**
 * A single deliberate contribution toward a goal. Kept append-only (no
 * "current saved amount" counter on GoalEntity itself) so the running total
 * is always a SUM over real, individually-dated entries -- the same
 * "derive, don't cache" principle used for envelope spend totals, and for
 * the same reason: a cached running total can drift from reality if a write
 * is ever missed or a contribution is later deleted; a SUM over real rows
 * cannot.
 */
@Entity(
    tableName = "goal_contributions",
    indices = [Index(value = ["goalId"])]
)
data class GoalContributionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "goalId")
    val goalId: String,

    @ColumnInfo(name = "amountMinorUnits")
    val amountMinorUnits: Long,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "contributedAtEpochMillis")
    val contributedAtEpochMillis: Long = System.currentTimeMillis()
)