package com.ipon.app.ui.screens.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.Category
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import com.ipon.app.util.MerchantClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AddTransactionUiState(
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val merchantRaw: String = "",
    val note: String = "",
    val suggestedCategory: Category? = null,
    val amountError: String? = null,
    val savedTransactionId: String? = null
)

class AddTransactionViewModel(
    private val repository: TransactionRepository,
    private val merchantClassifier: MerchantClassifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amountInput = value, amountError = null) }
    }

    fun onTypeChanged(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onMerchantChanged(value: String) {
        // Section 3: live, on-device classification as the merchant string is typed.
        // See KeywordRuleMerchantClassifier's header comment for this prototype's
        // honest scope versus the trained model described in the proposal.
        val suggestion = merchantClassifier.classify(value)
        _uiState.update {
            it.copy(
                merchantRaw = value,
                suggestedCategory = suggestion,
                selectedCategory = it.selectedCategory ?: suggestion
            )
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun save() {
        val state = _uiState.value
        val amount = Money.parse(state.amountInput)
        if (amount == null || amount.isZero) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }

        val category = state.selectedCategory ?: Category.OTHER
        val wasAutoCategorized = state.suggestedCategory != null && state.selectedCategory == state.suggestedCategory

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = state.type,
            category = category.displayName,
            merchantRaw = state.merchantRaw.ifBlank { null },
            note = state.note.ifBlank { null },
            occurredAtEpochMillis = System.currentTimeMillis(),
            isAutoCategorized = wasAutoCategorized
        )

        viewModelScope.launch {
            repository.add(transaction)
            _uiState.update { it.copy(savedTransactionId = transaction.id) }
        }
    }
}
