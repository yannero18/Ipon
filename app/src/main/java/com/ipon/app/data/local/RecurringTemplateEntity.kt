package com.ipon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RecurrenceFrequency { MONTHLY, WEEKLY }

/**
 * A recurring transaction TEMPLATE -- not an auto-posting rule.
 *
 * Deliberate design choice: this app never silently writes a transaction
 * into the ledger on the user's behalf. A "recurring" entry here means
 * "remind me and pre-fill the form," not "log this automatically while I'm
 * not looking." That distinction matters for a journal-style app built on
 * trust and zero hidden behavior -- the proposal's whole pitch is that the
 * user's device is "the single, uncompromising source of truth," which
 * implies the user, not a background job, is the one writing to it.
 *
 * [dayOfPeriod] means day-of-month (1-31, clamped to the last real day of
 * shorter months) for MONTHLY, or day-of-week (1=Monday..7=Sunday, ISO-8601
 * convention) for WEEKLY.
 *
 * [lastConfirmedPeriodKey] tracks the most recent period this template was
 * actually turned into a real transaction for, so the app can compute "is
 * this due" without re-deriving it from the transactions table (which
 * would require fuzzy-matching merchant strings -- unreliable). Format mirrors the
 * periodYearMonth convention from EnvelopeEntity ("YYYY-MM" for monthly,
 * "YYYY-Www" ISO week format for weekly).
 */
@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "amountMinorUnits")
    val amountMinorUnits: Long,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "merchantRaw")
    val merchantRaw: String?,

    @ColumnInfo(name = "frequency")
    val frequency: RecurrenceFrequency,

    @ColumnInfo(name = "dayOfPeriod")
    val dayOfPeriod: Int,

    @ColumnInfo(name = "lastConfirmedPeriodKey")
    val lastConfirmedPeriodKey: String?,

    @ColumnInfo(name = "isPaused")
    val isPaused: Boolean = false,

    @ColumnInfo(name = "createdAtEpochMillis")
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
