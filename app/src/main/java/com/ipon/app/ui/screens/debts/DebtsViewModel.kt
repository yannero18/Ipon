package com.ipon.app.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.Debt
import com.ipon.app.data.model.DebtProgress
import com.ipon.app.data.model.PayoffMethod
import com.ipon.app.data.model.orderedForPayoff
import com.ipon.app.data.repository.DebtRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val orderedDebts: List<DebtProgress> = emptyList(),
    val paidOffDebts: List<DebtProgress> = emptyList(),
    val method: PayoffMethod = PayoffMethod.SNOWBALL,
    val isLoading: Boolean = true
)

class DebtsViewModel(private val repository: DebtRepository) : ViewModel() {

    private val selectedMethod = MutableStateFlow(PayoffMethod.SNOWBALL)

    val uiState: StateFlow<DebtsUiState> = combine(
        repository.observeProgress(),
        selectedMethod
    ) { allProgress, method ->
        DebtsUiState(
            orderedDebts = allProgress.orderedForPayoff(method),
            paidOffDebts = allProgress.filter { it.isPaidOff },
            method = method,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtsUiState())

    fun setMethod(method: PayoffMethod) {
        selectedMethod.value = method
    }

    fun createDebt(label: String, originalBalance: Money, interestRatePercent: Double?, fee: Money, dueDate: String?) {
        viewModelScope.launch {
            repository.createDebt(label, originalBalance, interestRatePercent, fee, dueDate)
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            repository.updateDebt(debt)
        }
    }

    fun recordPayment(debt: Debt, amount: Money) {
        viewModelScope.launch {
            repository.recordPayment(debt.id, amount)
        }
    }

    fun archiveDebt(debt: Debt) {
        viewModelScope.launch {
            repository.archiveDebt(debt)
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }
}
