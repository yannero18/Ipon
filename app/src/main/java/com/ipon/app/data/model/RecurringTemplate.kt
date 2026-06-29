package com.ipon.app.data.model

import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.RecurringTemplateEntity
import com.ipon.app.data.local.TransactionType
import com.ipon.app.util.Money
import java.util.Calendar
import java.util.Locale

data class RecurringTemplate(
    val id: String,
    val label: String,
    val amount: Money,
    val type: TransactionType,
    val category: TransactionCategory,
    val merchantRaw: String?,
    val frequency: RecurrenceFrequency,
    val dayOfPeriod: Int,
    val lastConfirmedPeriodKey: String?,
    val isPaused: Boolean
) {
    /**
     * True if this template's current period has not yet been confirmed
     * into a real transaction AND today's date has reached/passed
     * [dayOfPeriod] for that period. A template due on the 15th doesn't
     * show as "due" on the 3rd -- it shows up exactly when it's time to act,
     * which is what makes this a helpful nudge rather than a wall of nagging
     * reminders for things three weeks away.
     */
    fun isDueAsOf(calendar: Calendar): Boolean {
        if (isPaused) return false
        val currentKey = currentPeriodKey(calendar)
        if (currentKey == lastConfirmedPeriodKey) return false

        return when (frequency) {
            RecurrenceFrequency.MONTHLY -> {
                val today = calendar.get(Calendar.DAY_OF_MONTH)
                val lastDayThisMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = dayOfPeriod.coerceAtMost(lastDayThisMonth)
                today >= effectiveDay
            }
            RecurrenceFrequency.WEEKLY -> {
                // Calendar.DAY_OF_WEEK is SUNDAY=1..SATURDAY=7; dayOfPeriod
                // is stored ISO-8601 style (MONDAY=1..SUNDAY=7), so convert.
                val isoToday = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
                isoToday >= dayOfPeriod
            }
        }
    }

    fun currentPeriodKey(calendar: Calendar): String = when (frequency) {
        RecurrenceFrequency.MONTHLY ->
            String.format(Locale.US, "%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        RecurrenceFrequency.WEEKLY ->
            String.format(Locale.US, "%04d-W%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.WEEK_OF_YEAR))
    }
}

fun RecurringTemplateEntity.toDomain(): RecurringTemplate = RecurringTemplate(
    id = id,
    label = label,
    amount = Money.ofMinorUnits(amountMinorUnits),
    type = type,
    category = resolveCategory(category, type),
    merchantRaw = merchantRaw,
    frequency = frequency,
    dayOfPeriod = dayOfPeriod,
    lastConfirmedPeriodKey = lastConfirmedPeriodKey,
    isPaused = isPaused
)

fun RecurringTemplate.toEntity(): RecurringTemplateEntity = RecurringTemplateEntity(
    id = id,
    label = label,
    amountMinorUnits = amount.minorUnits,
    type = type,
    category = category.displayName,
    merchantRaw = merchantRaw,
    frequency = frequency,
    dayOfPeriod = dayOfPeriod,
    lastConfirmedPeriodKey = lastConfirmedPeriodKey,
    isPaused = isPaused
)
