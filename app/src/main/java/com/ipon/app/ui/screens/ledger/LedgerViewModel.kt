package com.ipon.app.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.Transaction
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
    val isLoading: Boolean = true
)

class LedgerViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.observeBetween(monthRange.first, monthRange.second),
        repository.observeSummaryBetween(monthRange.first, monthRange.second)
    ) { transactions, summary ->
        LedgerUiState(transactions = transactions, summary = summary, isLoading = false)
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
