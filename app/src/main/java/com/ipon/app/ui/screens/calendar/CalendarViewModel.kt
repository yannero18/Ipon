package com.ipon.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.Debt
import com.ipon.app.data.model.DebtProgress
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.data.repository.DebtRepository
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

/**
 * One thing landing on a specific calendar day -- either a Recurring
 * template occurrence, or a Debt's due date. Both expose the same
 * label/amount/isIncome shape so the projection math and the day cell UI
 * can treat them uniformly without caring which kind they are.
 */
sealed interface CalendarDayEvent {
    val label: String
    val amount: Money
    val isIncome: Boolean

    data class Recurring(val template: RecurringTemplate) : CalendarDayEvent {
        override val label: String get() = template.label
        override val amount: Money get() = template.amount
        override val isIncome: Boolean get() = template.type == TransactionType.INCOME
    }

    /** [progress] carries the REMAINING balance, not the original -- a partly-paid-down debt shows what's actually still owed, same "derive, don't cache" principle as everywhere else. */
    data class DebtDue(val progress: DebtProgress) : CalendarDayEvent {
        override val label: String get() = "${progress.debt.label} due"
        override val amount: Money get() = progress.remaining
        override val isIncome: Boolean get() = false
    }
}

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
    debtRepository: DebtRepository,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<CalendarUiState> = combine(
        recurringTemplateRepository.observeAll(),
        debtRepository.observeProgress(),
        transactionRepository.observeSummaryBetween(monthRange.first, monthRange.second)
    ) { templates, debtProgresses, summary ->
        buildUiState(
            templates.filter { !it.isPaused },
            debtProgresses.filter { !it.debt.isArchived && !it.isPaidOff && it.debt.dueDate != null },
            summary.net
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    private fun buildUiState(
        templates: List<RecurringTemplate>,
        dueDebts: List<DebtProgress>,
        startingBalance: Money
    ): CalendarUiState {
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

            val recurringEvents: List<CalendarDayEvent> = templates
                .filter { isDueOnDay(it, cal, dayNum, lastDayOfMonth) }
                .map { CalendarDayEvent.Recurring(it) }
            val debtEvents: List<CalendarDayEvent> = dueDebts
                .filter { isDebtDueOnDay(it.debt, dayNum, lastDayOfMonth) }
                .map { CalendarDayEvent.DebtDue(it) }
            val eventsForDay = recurringEvents + debtEvents

            val projectedBalance: Money? = if (isPast) {
                null
            } else {
                val netMinorForDay = eventsForDay.sumOf {
                    if (it.isIncome) it.amount.minorUnits else -it.amount.minorUnits
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
     * Debt due dates are stored as ISO "yyyy-MM-dd" (see DebtsScreen.kt),
     * always a single lump-sum deadline, not a recurring occurrence -- so
     * this only ever matches once, unlike [isDueOnDay]'s repeating templates.
     */
    private fun isDebtDueOnDay(debt: Debt, dayNum: Int, lastDayOfMonth: Int): Boolean {
        val dueDate = debt.dueDate ?: return false
        val parts = dueDate.split("-")
        if (parts.size != 3) return false
        val dueYear = parts[0].toIntOrNull() ?: return false
        val dueMonth = parts[1].toIntOrNull() ?: return false
        val dueDay = parts[2].toIntOrNull() ?: return false

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        return dueYear == currentYear && dueMonth == currentMonth && dueDay.coerceAtMost(lastDayOfMonth) == dayNum
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
