package com.ipon.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.repository.CategorySlice
import com.ipon.app.data.repository.PeriodSummary
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class InsightsUiState(
    val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val isLoading: Boolean = true
)

class InsightsViewModel(repository: TransactionRepository) : ViewModel() {

    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeSummaryBetween(monthRange.first, monthRange.second),
        repository.observeCategoryBreakdownBetween(monthRange.first, monthRange.second)
    ) { summary, breakdown ->
        InsightsUiState(summary = summary, categoryBreakdown = breakdown, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

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
        return start to calendar.timeInMillis
    }
}
