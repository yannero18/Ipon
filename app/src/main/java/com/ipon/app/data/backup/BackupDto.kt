package com.ipon.app.data.backup

import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.TransactionType
import com.squareup.moshi.JsonClass

/**
 * The full contents of a real, restorable Ipon backup -- distinct from the
 * CSV export in ExportRepository, which is one-way and meant for opening
 * in a spreadsheet, not restoring from.
 *
 * These DTOs are a deliberately separate, independent contract from the
 * live Room [com.ipon.app.data.local.IponDatabase] entities, even though
 * today their fields line up 1:1. That separation is the actual point:
 * a future Room schema change (a column rename, a table split, whatever)
 * changes IponDatabase without silently changing what a backup file looks
 * like, and vice versa -- a backup taken today stays readable by future
 * versions of this app on its own terms, not by accident.
 *
 * Compatibility strategy:
 * - [IponBackup.schemaVersion] identifies the shape of this JSON file.
 *   Bump it only for a genuinely BREAKING change (a field renamed, removed,
 *   or changed type/meaning) -- something Moshi's default parsing can't
 *   absorb on its own. [BackupRepository] checks this before importing and
 *   refuses anything newer than it understands, rather than guessing.
 * - Adding a new field to any DTO here is NOT a breaking change: every
 *   field below has a real default, so a JSON file written before that
 *   field existed still parses cleanly (Moshi fills in the default for
 *   whatever key is missing) -- no version bump needed for purely
 *   additive changes. This is the concrete mechanism, not just the version
 *   number by itself, that answers "what happens when we add a feature the
 *   backup format doesn't know about yet."
 * - Amounts stay in minor units (Long centavos), same as everywhere else
 *   in this codebase -- never a Double for money, see Money.kt.
 */
const val CURRENT_BACKUP_SCHEMA_VERSION = 1

@JsonClass(generateAdapter = true)
data class IponBackup(
    val schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    /** Room's IponDatabase.version at export time -- informational only, not used for compatibility decisions. */
    val appDatabaseVersion: Int,
    val transactions: List<TransactionBackupDto> = emptyList(),
    val envelopes: List<EnvelopeBackupDto> = emptyList(),
    val recurringTemplates: List<RecurringTemplateBackupDto> = emptyList(),
    val goals: List<GoalBackupDto> = emptyList(),
    val goalContributions: List<GoalContributionBackupDto> = emptyList(),
    val debts: List<DebtBackupDto> = emptyList(),
    val debtPayments: List<DebtPaymentBackupDto> = emptyList(),
    val dailyReflections: List<DailyReflectionBackupDto> = emptyList(),
    val merchantCategoryMemories: List<MerchantCategoryMemoryBackupDto> = emptyList(),
    val reports: List<ReportBackupDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TransactionBackupDto(
    val id: String,
    val amountMinorUnits: Long,
    val type: TransactionType,
    val category: String,
    val merchantRaw: String? = null,
    val note: String? = null,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val isAutoCategorized: Boolean = false
)

@JsonClass(generateAdapter = true)
data class EnvelopeBackupDto(
    val id: String,
    val category: String,
    val periodYearMonth: String,
    val capMinorUnits: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val priority: Int = 2,
    val customIcon: String? = null
)

@JsonClass(generateAdapter = true)
data class RecurringTemplateBackupDto(
    val id: String,
    val label: String,
    val amountMinorUnits: Long,
    val type: TransactionType,
    val category: String,
    val merchantRaw: String? = null,
    val frequency: RecurrenceFrequency,
    val dayOfPeriod: Int,
    val lastConfirmedPeriodKey: String? = null,
    val isPaused: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class GoalBackupDto(
    val id: String,
    val label: String,
    val targetMinorUnits: Long,
    val emoji: String,
    val isArchived: Boolean = false,
    val deadline: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /** Added Phase 12 alongside the GoSave-style redesign; null for goals created before then. */
    val imageUri: String? = null
)

@JsonClass(generateAdapter = true)
data class GoalContributionBackupDto(
    val id: String,
    val goalId: String,
    val amountMinorUnits: Long,
    val note: String? = null,
    val contributedAtEpochMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DebtBackupDto(
    val id: String,
    val label: String,
    val originalBalanceMinorUnits: Long,
    val interestRatePercent: Double? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DebtPaymentBackupDto(
    val id: String,
    val debtId: String,
    val amountMinorUnits: Long,
    val paidAtEpochMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DailyReflectionBackupDto(
    val id: String,
    val dayKey: String,
    val mood: MoodRating,
    val note: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class MerchantCategoryMemoryBackupDto(
    val merchantKey: String,
    val type: TransactionType,
    val category: String,
    val confirmCount: Int = 1,
    val lastUsedEpochMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ReportBackupDto(
    val id: String,
    val periodYearMonth: String,
    val title: String,
    val content: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
