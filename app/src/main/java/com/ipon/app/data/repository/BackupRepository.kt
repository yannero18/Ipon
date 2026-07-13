package com.ipon.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.ipon.app.data.backup.CURRENT_BACKUP_SCHEMA_VERSION
import com.ipon.app.data.backup.IponBackup
import com.ipon.app.data.backup.toBackupDto
import com.ipon.app.data.backup.toEntity
import com.ipon.app.data.local.IponDatabase
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

/** Row counts from a successful restore, so the confirmation UI can show something concrete rather than just "done." */
data class BackupSummary(
    val transactionCount: Int,
    val envelopeCount: Int,
    val recurringTemplateCount: Int,
    val goalCount: Int,
    val goalContributionCount: Int,
    val debtCount: Int,
    val debtPaymentCount: Int,
    val dailyReflectionCount: Int,
    val merchantMemoryCount: Int,
    val reportCount: Int
) {
    val totalRows: Int
        get() = transactionCount + envelopeCount + recurringTemplateCount + goalCount + goalContributionCount +
            debtCount + debtPaymentCount + dailyReflectionCount + merchantMemoryCount + reportCount
}

/** A backup file whose schemaVersion is higher than this build of Ipon understands -- e.g. restoring a future version's backup onto an older app install. */
class BackupTooNewException(val fileSchemaVersion: Int) : Exception(
    "This backup was made by a newer version of Ipon (schema v$fileSchemaVersion) than this app understands (v$CURRENT_BACKUP_SCHEMA_VERSION)."
)

/** The file wasn't valid JSON, or didn't match the expected backup shape at all. */
class InvalidBackupFileException(cause: Throwable) : Exception("This doesn't look like a valid Ipon backup file.", cause)

/**
 * A real, restorable backup -- distinct from [ExportRepository]'s CSV export,
 * which is one-way and meant for a spreadsheet, not for restoring from.
 *
 * Restoring is destructive by design: it clears every table and replaces it
 * with the backup's contents, inside a single Room transaction so a restore
 * either fully succeeds or leaves the database exactly as it was -- never a
 * half-applied mix of old and new data. The UI layer is responsible for a
 * clear, explicit "this replaces everything currently on this device"
 * confirmation before calling [importBackup], the same way EditGoalScreen's
 * delete confirmation works -- this repository does not ask twice, it trusts
 * the caller already did.
 */
class BackupRepository(
    private val context: Context,
    private val database: IponDatabase,
    private val moshi: Moshi = Moshi.Builder().build()
) {
    private val backupAdapter = moshi.adapter(IponBackup::class.java)

    suspend fun exportBackup(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw UnsupportedOperationException("Backup export requires Android 10 (API 29) or newer.")
        }

        val backup = IponBackup(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAtEpochMillis = System.currentTimeMillis(),
            appDatabaseVersion = database.openHelper.readableDatabase.version,
            transactions = database.transactionDao().observeAll().first().map { it.toBackupDto() },
            envelopes = database.envelopeDao().getAllEver().map { it.toBackupDto() },
            recurringTemplates = database.recurringTemplateDao().observeAll().first().map { it.toBackupDto() },
            goals = database.goalDao().getAllGoalsEver().map { it.toBackupDto() },
            goalContributions = database.goalDao().observeAllContributions().first().map { it.toBackupDto() },
            debts = database.debtDao().getAllDebtsEver().map { it.toBackupDto() },
            debtPayments = database.debtDao().getAllDebtPaymentsEver().map { it.toBackupDto() },
            dailyReflections = database.dailyReflectionDao().observeRecent(Int.MAX_VALUE).first().map { it.toBackupDto() },
            merchantCategoryMemories = database.merchantCategoryMemoryDao().observeAll().first().map { it.toBackupDto() },
            reports = database.reportDao().getAllReports().first().map { it.toBackupDto() }
        )

        val json = backupAdapter.toJson(backup)
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(System.currentTimeMillis())
        val filename = "ipon_backup_$timestamp.json"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("MediaStore did not return a URI for the new backup file.")
        val stream = resolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open an output stream for the backup file.")
        stream.use { it.write(json.toByteArray()) }

        return filename
    }

    /**
     * Reads and validates [uri] WITHOUT touching the database -- lets the UI
     * show "this backup has 214 transactions, from July 2026" before the
     * person commits to the destructive restore, rather than committing
     * blind.
     */
    suspend fun peekBackup(uri: Uri): IponBackup {
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IllegalStateException("Could not read the selected file.")
        val backup = try {
            backupAdapter.fromJson(json)
        } catch (e: Exception) {
            throw InvalidBackupFileException(e)
        } ?: throw InvalidBackupFileException(IllegalStateException("Empty or malformed JSON."))

        if (backup.schemaVersion > CURRENT_BACKUP_SCHEMA_VERSION) {
            throw BackupTooNewException(backup.schemaVersion)
        }
        return backup
    }

    /** Actually performs the restore. Call [peekBackup] first to validate and let the person confirm. */
    suspend fun importBackup(backup: IponBackup): BackupSummary {
        database.withTransaction {
            database.clearAllTables()

            backup.transactions.forEach { database.transactionDao().insert(it.toEntity()) }
            backup.envelopes.forEach { database.envelopeDao().upsert(it.toEntity()) }
            backup.recurringTemplates.forEach { database.recurringTemplateDao().insert(it.toEntity()) }
            backup.goals.forEach { database.goalDao().insert(it.toEntity()) }
            backup.goalContributions.forEach { database.goalDao().insertContribution(it.toEntity()) }
            backup.debts.forEach { database.debtDao().insert(it.toEntity()) }
            backup.debtPayments.forEach { database.debtDao().insertPayment(it.toEntity()) }
            backup.dailyReflections.forEach { database.dailyReflectionDao().upsert(it.toEntity()) }
            backup.merchantCategoryMemories.forEach { database.merchantCategoryMemoryDao().upsert(it.toEntity()) }
            backup.reports.forEach { database.reportDao().insertReport(it.toEntity()) }
        }

        return BackupSummary(
            transactionCount = backup.transactions.size,
            envelopeCount = backup.envelopes.size,
            recurringTemplateCount = backup.recurringTemplates.size,
            goalCount = backup.goals.size,
            goalContributionCount = backup.goalContributions.size,
            debtCount = backup.debts.size,
            debtPaymentCount = backup.debtPayments.size,
            dailyReflectionCount = backup.dailyReflections.size,
            merchantMemoryCount = backup.merchantCategoryMemories.size,
            reportCount = backup.reports.size
        )
    }
}
