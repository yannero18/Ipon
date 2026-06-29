package com.ipon.app.ui.screens.budgetplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.FiftyThirtyTwentyComparison
import com.ipon.app.data.model.ZeroBasedBudget
import com.ipon.app.data.repository.BudgetPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Locale

data class BudgetPlanUiState(
    val fiftyThirtyTwenty: FiftyThirtyTwentyComparison? = null,
    val zeroBased: ZeroBasedBudget? = null,
    val isLoading: Boolean = true
)

class BudgetPlanViewModel(repository: BudgetPlanRepository) : ViewModel() {

    private val monthRange = currentMonthRangeMillis()
    private val periodKey = currentYearMonth()

    val uiState: StateFlow<BudgetPlanUiState> = combine(
        repository.observeFiftyThirtyTwenty(monthRange.first, monthRange.second),
        repository.observeZeroBasedBudget(periodKey, monthRange.first, monthRange.second)
    ) { fiftyThirtyTwenty, zeroBased ->
        BudgetPlanUiState(fiftyThirtyTwenty = fiftyThirtyTwenty, zeroBased = zeroBased, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetPlanUiState())

    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
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
        return start to calendar.timeInMillis
    }
}
