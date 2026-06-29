package com.ipon.app.data.model

import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.TransactionType
import com.ipon.app.util.Money
import com.ipon.app.util.normalizeMerchantKey
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * A detected repeating pattern in the user's own transaction history,
 * surfaced as "want to turn this into a Recurring template?" Like
 * [SavingsSuggestion] and the category memory system, this is plain
 * grouping/counting logic over real transactions -- no model, no
 * cross-user data, nothing that leaves the device.
 */
data class RecurringPatternSuggestion(
    val merchantKey: String,
    val merchantRawExample: String,
    val category: TransactionCategory,
    val type: TransactionType,
    val averageAmount: Money,
    val occurrenceCount: Int,
    val suggestedFrequency: RecurrenceFrequency
)

/**
 * Scans [transactions] for merchant strings that recur on a roughly
 * monthly or weekly cadence with a similar amount each time, and are not
 * already covered by an existing template (matched on normalized merchant
 * key, in [existingTemplateMerchantKeys]).
 *
 * Deliberately conservative thresholds -- this is a once-in-a-while nudge,
 * not a feature that fires constantly:
 * - Requires at least 3 occurrences (one or two could be coincidence)
 * - Requires the amounts to be within 15% of their own average (otherwise
 *   it's not really "the same bill," just the same merchant)
 * - Requires the gaps between occurrences to be roughly consistent with
 *   either a weekly (5-9 day) or monthly (25-35 day) cadence
 */
fun detectRecurringPatterns(
    transactions: List<Transaction>,
    existingTemplateMerchantKeys: Set<String>
): List<RecurringPatternSuggestion> {
    val withMerchant = transactions.filter { !it.merchantRaw.isNullOrBlank() }

    val grouped = withMerchant.groupBy { tx ->
        normalizeMerchantKey(tx.merchantRaw!!) to tx.type
    }

    return grouped.mapNotNull { (key, group) ->
        val (merchantKey, type) = key
        if (merchantKey.isEmpty() || merchantKey in existingTemplateMerchantKeys) return@mapNotNull null
        if (group.size < 3) return@mapNotNull null

        val sorted = group.sortedBy { it.occurredAtEpochMillis }
        val amounts = sorted.map { it.amount.minorUnits }
        val average = amounts.average()
        val withinTolerance = amounts.all { amount -> abs(amount - average) <= average * 0.15 }
        if (!withinTolerance) return@mapNotNull null

        val gapsInDays = sorted.zipWithNext { a, b ->
            TimeUnit.MILLISECONDS.toDays(b.occurredAtEpochMillis - a.occurredAtEpochMillis)
        }
        val averageGap = gapsInDays.average()

        val frequency = when {
            averageGap in 25.0..35.0 -> RecurrenceFrequency.MONTHLY
            averageGap in 5.0..9.0 -> RecurrenceFrequency.WEEKLY
            else -> return@mapNotNull null
        }

        RecurringPatternSuggestion(
            merchantKey = merchantKey,
            merchantRawExample = sorted.last().merchantRaw!!,
            category = resolveCategory(sorted.last().category, type),
            type = type,
            averageAmount = Money.ofMinorUnits(average.toLong()),
            occurrenceCount = sorted.size,
            suggestedFrequency = frequency
        )
    }.sortedByDescending { it.occurrenceCount }
}
