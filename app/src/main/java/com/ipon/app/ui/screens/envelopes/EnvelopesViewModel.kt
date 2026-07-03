package com.ipon.app.ui.screens.envelopes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.repository.EnvelopeRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class EnvelopesUiState(
    val envelopes: List<EnvelopeProgress> = emptyList(),
    /** Average spend over the last 3 complete months, by category display name -- used to suggest a starting cap rather than asking the person to guess from nothing. */
    val suggestedCapsByCategory: Map<String, Money> = emptyMap(),
    val isLoading: Boolean = true
)

class EnvelopesViewModel(
    private val repository: EnvelopeRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val currentPeriod = currentYearMonth()
    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<EnvelopesUiState> = combine(
        repository.observeProgressForPeriod(currentPeriod, monthRange.first, monthRange.second),
        transactionRepository.observeCategoryAverages(monthCount = 3)
    ) { progress, averages ->
        EnvelopesUiState(
            envelopes = progress,
            suggestedCapsByCategory = averages.associate { it.category to it.total },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EnvelopesUiState())

    fun setCap(category: ExpenseCategory, amount: Money, customIcon: String? = null) {
        viewModelScope.launch {
            repository.setCap(category, currentPeriod, amount, customIcon)
        }
    }

    fun removeCap(category: ExpenseCategory) {
        viewModelScope.launch {
            repository.removeCap(category, currentPeriod)
        }
    }

    fun copyForwardFromLastMonth() {
        viewModelScope.launch {
            repository.copyForwardFrom(previousYearMonth(), currentPeriod)
        }
    }

    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun previousYearMonth(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
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
