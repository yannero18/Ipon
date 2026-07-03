package com.ipon.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ipon.app.data.local.IponDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Exports every table Settings' "clear all data" would wipe into a single
 * human-readable CSV, written to the device's public Downloads folder via
 * MediaStore -- not app-private storage, so the file survives even if the
 * app is later uninstalled, and the person can actually find it without
 * a file-manager app that can browse app-internal storage.
 *
 * Deliberately CSV, not a database file or JSON: openable in any
 * spreadsheet app on the person's phone with zero extra tooling, which
 * matters more here than perfect machine-readability -- this app has no
 * import feature (and isn't getting one in this pass), so the export's
 * only real job is "can the person see and keep their own data," not
 * "can this be re-imported automatically."
 *
 * Uses MediaStore.Downloads (API 29+) rather than raw file paths, since
 * writing directly to external storage paths needs broad storage
 * permissions on older Android versions that this app would otherwise
 * never need to request -- everything else in Ipon works with zero
 * permissions beyond VIBRATE, and this export should not be the one
 * feature that forces a storage permission prompt onto everyone.
 */
class ExportRepository(
    private val context: Context,
    private val database: IponDatabase
) {

    /**
     * Returns the display filename of the exported file on success.
     * Throws [UnsupportedOperationException] specifically when running on
     * an API level below 29, where MediaStore.Downloads isn't available --
     * kept distinct from a generic write failure so SettingsViewModel can
     * tell the person "this isn't available on your Android version"
     * rather than a generic "export failed," which would be confusing if
     * it happens every single time on an old device.
     */
    suspend fun exportAllDataToCsv(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw UnsupportedOperationException("CSV export requires Android 10 (API 29) or newer.")
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(System.currentTimeMillis())
        val filename = "ipon_export_$timestamp.csv"
        val csvContent = buildCsvContent()

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("MediaStore did not return a URI for the new export file.")

        val stream = resolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open an output stream for the export file.")
        stream.use { it.write(csvContent.toByteArray()) }

        return filename
    }

    private suspend fun buildCsvContent(): String {
        val builder = StringBuilder()
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        builder.appendLine("# IPON DATA EXPORT")
        builder.appendLine("# Generated ${dateTimeFormat.format(System.currentTimeMillis())}")
        builder.appendLine("# This file contains everything stored in Ipon on this device.")
        builder.appendLine()

        builder.appendLine("## TRANSACTIONS")
        builder.appendLine("id,type,category,amount,merchant,note,date,auto_categorized")
        database.transactionDao().observeAll().first().forEach { tx ->
            builder.appendLine(
                listOf(
                    tx.id,
                    tx.type.name,
                    csvEscape(tx.category),
                    (tx.amountMinorUnits / 100.0).toString(),
                    csvEscape(tx.merchantRaw.orEmpty()),
                    csvEscape(tx.note.orEmpty()),
                    dateTimeFormat.format(tx.occurredAtEpochMillis),
                    tx.isAutoCategorized.toString()
                ).joinToString(",")
            )
        }
        builder.appendLine()

        builder.appendLine("## ENVELOPES (all months)")
        builder.appendLine("category,period,cap")
        database.envelopeDao().getAllForPeriodOnce("").forEach { env -> // getAllForPeriodOnce("") returns all because of wildcard in envelope SQL
            builder.appendLine(
                listOf(csvEscape(env.category), env.periodYearMonth, (env.capMinorUnits / 100.0).toString())
                    .joinToString(",")
            )
        }
        builder.appendLine()

        builder.appendLine("## RECURRING TEMPLATES")
        builder.appendLine("label,type,category,amount,frequency,day_of_period,paused")
        database.recurringTemplateDao().observeAll().first().forEach { template ->
            builder.appendLine(
                listOf(
                    csvEscape(template.label),
                    template.type.name,
                    csvEscape(template.category),
                    (template.amountMinorUnits / 100.0).toString(),
                    template.frequency.name,
                    template.dayOfPeriod.toString(),
                    template.isPaused.toString()
                ).joinToString(",")
            )
        }
        builder.appendLine()

        builder.appendLine("## GOALS")
        builder.appendLine("label,target,emoji,archived")
        database.goalDao().observeActive().first().forEach { goal ->
            builder.appendLine(
                listOf(csvEscape(goal.label), (goal.targetMinorUnits / 100.0).toString(), goal.emoji, goal.isArchived.toString())
                    .joinToString(",")
            )
        }
        builder.appendLine()

        builder.appendLine("## DAILY REFLECTIONS")
        builder.appendLine("date,mood,note")
        database.dailyReflectionDao().observeRecent(Int.MAX_VALUE).first().forEach { reflection ->
            builder.appendLine(
                listOf(reflection.dayKey, reflection.mood.name, csvEscape(reflection.note.orEmpty()))
                    .joinToString(",")
            )
        }

        return builder.toString()
    }

    /** Wraps a value in quotes and escapes embedded quotes/commas/newlines, the minimum needed for valid CSV. */
    private fun csvEscape(value: String): String {
        if (value.isEmpty()) return ""
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}
