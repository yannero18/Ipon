package com.ipon.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.CategoryTrend
import com.ipon.app.data.model.SavingsSuggestion
import com.ipon.app.data.model.generateSavingsSuggestions
import com.ipon.app.data.model.CategorySlice
import com.ipon.app.data.repository.EnvelopeRepository
import com.ipon.app.data.repository.PeriodSummary
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Locale

data class InsightsUiState(
    val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val trends: List<CategoryTrend> = emptyList(),
    val suggestions: List<SavingsSuggestion> = emptyList(),
    val isLoading: Boolean = true
)

class InsightsViewModel(
    transactionRepository: TransactionRepository,
    envelopeRepository: EnvelopeRepository
) : ViewModel() {

    private val thisMonthRange = monthRangeMillis(monthOffset = 0)
    private val lastMonthRange = monthRangeMillis(monthOffset = -1)
    private val currentPeriodKey = currentYearMonth()

    val uiState: StateFlow<InsightsUiState> = combine(
        transactionRepository.observeSummaryBetween(thisMonthRange.first, thisMonthRange.second),
        transactionRepository.observeCategoryBreakdownBetween(thisMonthRange.first, thisMonthRange.second),
        transactionRepository.observeCategoryTrends(
            thisMonthStart = thisMonthRange.first,
            thisMonthEnd = thisMonthRange.second,
            lastMonthStart = lastMonthRange.first,
            lastMonthEnd = lastMonthRange.second
        ),
        envelopeRepository.observeProgressForPeriod(
            periodYearMonth = currentPeriodKey,
            startEpochMillis = thisMonthRange.first,
            endEpochMillis = thisMonthRange.second
        )
    ) { summary, breakdown, trends, envelopeProgress ->
        val filteredTrends = trends
            // Trends only matter for categories you actually spent on this
            // month -- a category that's zero in both months is noise, not
            // an insight, so it's filtered out before reaching the UI.
            .filter { !it.thisMonth.isZero || !it.lastMonth.isZero }
            .sortedByDescending { it.thisMonth.minorUnits }

        InsightsUiState(
            summary = summary,
            categoryBreakdown = breakdown,
            trends = filteredTrends,
            suggestions = generateSavingsSuggestions(
                trends = filteredTrends,
                envelopeProgress = envelopeProgress,
                totalIncome = summary.income,
                totalExpense = summary.expense
            ),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun monthRangeMillis(monthOffset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return start to calendar.timeInMillis
    }
}
