package com.ipon.app.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.estimateDaysOfRunway
import com.ipon.app.data.repository.PeriodSummary
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class LedgerUiState(
    val transactions: List<Transaction> = emptyList(),
    val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO),
    /** Estimated days until this month's balance hits zero at the current daily spend rate. Null when there's too little spend history to extrapolate, or the balance is already gone. */
    val estimatedDaysOfRunway: Int? = null,
    val isLoading: Boolean = true
)

class LedgerViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.observeBetween(monthRange.first, monthRange.second),
        repository.observeSummaryBetween(monthRange.first, monthRange.second)
    ) { transactions, summary ->
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val averageDailySpend = if (dayOfMonth > 0) {
            Money.ofMinorUnits(summary.expense.minorUnits / dayOfMonth)
        } else {
            Money.ZERO
        }

        LedgerUiState(
            transactions = transactions,
            summary = summary,
            // Only worth surfacing a few days into the month -- on day 1
            // or 2 the daily average is too noisy (one big purchase can
            // make it look like the month's balance will vanish in days)
            // to be a useful signal rather than a false alarm.
            estimatedDaysOfRunway = if (dayOfMonth >= 3) {
                estimateDaysOfRunway(summary.net, averageDailySpend)
            } else {
                null
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerUiState()
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
        val end = calendar.timeInMillis

        return start to end
    }
}
