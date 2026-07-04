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

class ExportRepository(
    private val context: Context,
    private val database: IponDatabase
) {
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

        // 1. TRANSACTIONS
        builder.appendLine("## TRANSACTIONS")
        builder.appendLine("id,type,category,amount,merchant,note,date,auto_categorized")
        database.transactionDao().observeAll().first().forEach { tx ->
            builder.appendLine(
                listOf(
                    tx.id, tx.type.name, csvEscape(tx.category), (tx.amountMinorUnits / 100.0).toString(),
                    csvEscape(tx.merchantRaw.orEmpty()), csvEscape(tx.note.orEmpty()), 
                    dateTimeFormat.format(tx.occurredAtEpochMillis), tx.isAutoCategorized.toString()
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 2. ENVELOPES
        builder.appendLine("## ENVELOPES (all months)")
        builder.appendLine("id,category,period,cap,priority,custom_icon")
        database.envelopeDao().getAllEver().forEach { env -> 
            builder.appendLine(
                listOf(
                    env.id, csvEscape(env.category), env.periodYearMonth, (env.capMinorUnits / 100.0).toString(),
                    env.priority.toString(), csvEscape(env.customIcon.orEmpty())
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 3. RECURRING TEMPLATES
        builder.appendLine("## RECURRING TEMPLATES")
        builder.appendLine("id,label,type,category,amount,frequency,day_of_period,paused")
        database.recurringTemplateDao().observeAll().first().forEach { template ->
            builder.appendLine(
                listOf(
                    template.id, csvEscape(template.label), template.type.name, csvEscape(template.category), 
                    (template.amountMinorUnits / 100.0).toString(), template.frequency.name, 
                    template.dayOfPeriod.toString(), template.isPaused.toString()
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 4. GOALS
        builder.appendLine("## GOALS")
        builder.appendLine("id,label,target,emoji,deadline,archived")
        database.goalDao().getAllGoalsEver().forEach { goal ->
            builder.appendLine(
                listOf(
                    goal.id, csvEscape(goal.label), (goal.targetMinorUnits / 100.0).toString(), 
                    goal.emoji, csvEscape(goal.deadline.orEmpty()), goal.isArchived.toString()
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 5. GOAL CONTRIBUTIONS
        builder.appendLine("## GOAL CONTRIBUTIONS")
        builder.appendLine("id,goal_id,amount,note,date")
        database.goalDao().observeAllContributions().first().forEach { contrib ->
            builder.appendLine(
                listOf(
                    contrib.id, contrib.goalId, (contrib.amountMinorUnits / 100.0).toString(), 
                    csvEscape(contrib.note.orEmpty()), dateTimeFormat.format(contrib.contributedAtEpochMillis)
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 6. DEBTS
        builder.appendLine("## DEBTS")
        builder.appendLine("id,label,original_balance,interest_rate,archived")
        database.debtDao().getAllDebtsEver().forEach { debt ->
            builder.appendLine(
                listOf(
                    debt.id, csvEscape(debt.label), (debt.originalBalanceMinorUnits / 100.0).toString(), 
                    debt.interestRatePercent?.toString() ?: "", debt.isArchived.toString()
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 7. DEBT PAYMENTS
        builder.appendLine("## DEBT PAYMENTS")
        builder.appendLine("id,debt_id,amount,date")
        database.debtDao().getAllDebtPaymentsEver().forEach { pay ->
            builder.appendLine(
                listOf(
                    pay.id, pay.debtId, (pay.amountMinorUnits / 100.0).toString(), dateTimeFormat.format(pay.paidAtEpochMillis)
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 8. DAILY REFLECTIONS
        builder.appendLine("## DAILY REFLECTIONS")
        builder.appendLine("id,date,mood,note")
        database.dailyReflectionDao().observeRecent(Int.MAX_VALUE).first().forEach { reflection ->
            builder.appendLine(
                listOf(reflection.id, reflection.dayKey, reflection.mood.name, csvEscape(reflection.note.orEmpty())).joinToString(",")
            )
        }
        builder.appendLine()

        // 9. LEARNED CATEGORIES
        builder.appendLine("## LEARNED CATEGORIES")
        builder.appendLine("merchant_key,type,category,confirm_count,last_used")
        database.merchantCategoryMemoryDao().observeAll().first().forEach { mem ->
            builder.appendLine(
                listOf(
                    csvEscape(mem.merchantKey), mem.type.name, csvEscape(mem.category), 
                    mem.confirmCount.toString(), dateTimeFormat.format(mem.lastUsedEpochMillis)
                ).joinToString(",")
            )
        }
        builder.appendLine()

        // 10. REPORTS
        builder.appendLine("## REPORTS")
        builder.appendLine("id,period,title,date")
        database.reportDao().getAllReports().first().forEach { rep ->
            builder.appendLine(
                listOf(rep.id, rep.periodYearMonth, csvEscape(rep.title), dateTimeFormat.format(rep.createdAtEpochMillis)).joinToString(",")
            )
        }

        return builder.toString()
    }

    private fun csvEscape(value: String): String {
        if (value.isEmpty()) return ""
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}