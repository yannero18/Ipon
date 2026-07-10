package com.ipon.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.data.repository.RecurringTemplateRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import com.ipon.app.util.OnboardingPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One recurring template landing on a specific calendar day. */
data class CalendarDayEvent(val template: RecurringTemplate)

data class CalendarDay(
    val dayOfMonth: Int,
    val isInCurrentMonth: Boolean,
    val isToday: Boolean,
    val isPast: Boolean,
    val isPayday: Boolean,
    val events: List<CalendarDayEvent>,
    /** Null for days before today (and for blank padding cells) -- the forecast only ever looks forward. */
    val projectedBalance: Money?
)

data class CalendarUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val weeks: List<List<CalendarDay>> = emptyList(),
    val startingBalance: Money = Money.ZERO,
    val endOfMonthProjectedBalance: Money = Money.ZERO
)

private val BLANK_DAY = CalendarDay(
    dayOfMonth = 0,
    isInCurrentMonth = false,
    isToday = false,
    isPast = false,
    isPayday = false,
    events = emptyList(),
    projectedBalance = null
)

/**
 * Projects the rest of the current month forward day by day using only two
 * honest, already-on-device sources: configured paydays (a day-of-month
 * marker with no stored amount -- see OnboardingPreferences) and active
 * Recurring templates (which DO have amounts, for both income and expense).
 * Nothing here fabricates a number: a payday shows as a marker because
 * Ipon doesn't know your paycheck amount unless you've also modeled it as
 * a recurring INCOME template, in which case it feeds the projection like
 * any other template would.
 *
 * Scoped to the current calendar month only for now -- see DEVLOG for why
 * that's a deliberate v1 cut, not an oversight.
 */
class CalendarViewModel(
    transactionRepository: TransactionRepository,
    recurringTemplateRepository: RecurringTemplateRepository,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<CalendarUiState> = combine(
        recurringTemplateRepository.observeAll(),
        transactionRepository.observeSummaryBetween(monthRange.first, monthRange.second)
    ) { templates, summary ->
        buildUiState(templates.filter { !it.isPaused }, summary.net)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    private fun buildUiState(templates: List<RecurringTemplate>, startingBalance: Money): CalendarUiState {
        val paydays = onboardingPreferences.getPaydays().toSet()
        val today = Calendar.getInstance()
        val todayDayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(today.time)

        val firstOfMonth = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lastDayOfMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        val leadingBlankCount = firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

        val days = mutableListOf<CalendarDay>()
        var runningBalance = startingBalance

        for (dayNum in 1..lastDayOfMonth) {
            val cal = (firstOfMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNum) }
            val isPast = dayNum < todayDayOfMonth
            val isToday = dayNum == todayDayOfMonth

            val eventsForDay = templates
                .filter { isDueOnDay(it, cal, dayNum, lastDayOfMonth) }
                .map { CalendarDayEvent(it) }

            val projectedBalance: Money? = if (isPast) {
                null
            } else {
                val netMinorForDay = eventsForDay.sumOf {
                    if (it.template.type == TransactionType.INCOME) it.template.amount.minorUnits else -it.template.amount.minorUnits
                }
                runningBalance = Money.ofMinorUnits(runningBalance.minorUnits + netMinorForDay)
                runningBalance
            }

            days.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    isInCurrentMonth = true,
                    isToday = isToday,
                    isPast = isPast,
                    isPayday = paydays.contains(dayNum),
                    events = eventsForDay,
                    projectedBalance = projectedBalance
                )
            )
        }

        val allCells: List<CalendarDay> = List(leadingBlankCount) { BLANK_DAY } + days
        val trailingBlankCount = (7 - allCells.size % 7) % 7
        val paddedCells = allCells + List(trailingBlankCount) { BLANK_DAY }
        val weeks = paddedCells.chunked(7)

        return CalendarUiState(
            isLoading = false,
            monthLabel = monthLabel,
            weeks = weeks,
            startingBalance = startingBalance,
            endOfMonthProjectedBalance = days.lastOrNull()?.projectedBalance ?: startingBalance
        )
    }

    /**
     * Whether [template] has an occurrence landing on [dayNum], skipping
     * the occurrence whose period has already been turned into a real
     * transaction (see RecurringTemplate.lastConfirmedPeriodKey) so a
     * confirmed bill doesn't show up twice: once as an actual transaction
     * (already inside [startingBalance]) and again as a projected one.
     */
    private fun isDueOnDay(template: RecurringTemplate, cal: Calendar, dayNum: Int, lastDayOfMonth: Int): Boolean {
        val periodKey = template.currentPeriodKey(cal)
        if (periodKey == template.lastConfirmedPeriodKey) return false

        return when (template.frequency) {
            RecurrenceFrequency.MONTHLY -> template.dayOfPeriod.coerceAtMost(lastDayOfMonth) == dayNum
            RecurrenceFrequency.WEEKLY -> {
                val isoDayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
                isoDayOfWeek == template.dayOfPeriod
            }
        }
    }

    private fun currentMonthRangeMillis(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis

        return start to end
    }
}
