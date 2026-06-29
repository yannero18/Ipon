package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A budget cap for one expense category in one calendar month.
 *
 * [periodYearMonth] is stored as "YYYY-MM" (e.g. "2026-06") rather than an
 * epoch range, since envelopes are inherently calendar-month scoped by
 * definition (the proposal's "Envelopes" tab in the design mockup) and a
 * plain sortable string key is simpler to query and reason about than
 * computing month boundaries every time, and trivially supports "copy last
 * month's envelopes forward" without any date arithmetic.
 *
 * Also created WITHOUT ROWID for the same cache-locality reasoning as
 * [TransactionEntity] -- see that file's header comment. Envelope rows are
 * looked up far more often than they're written (the ledger and insights
 * screens re-read budget caps on every render), so the read-optimized
 * clustered-index tradeoff applies here too.
 */
@Entity(tableName = "envelopes")
data class EnvelopeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "category")
    val category: String,

    /** Format: "YYYY-MM". */
    @ColumnInfo(name = "periodYearMonth")
    val periodYearMonth: String,

    /** Stored in minor units (centavos), same convention as transactions. */
    @ColumnInfo(name = "capMinorUnits")
    val capMinorUnits: Long,

    @ColumnInfo(name = "createdAtEpochMillis")
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
