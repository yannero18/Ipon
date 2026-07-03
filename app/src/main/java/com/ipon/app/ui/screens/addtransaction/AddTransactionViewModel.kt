package com.ipon.app.ui.screens.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.data.model.resolveCategory
import com.ipon.app.data.repository.CategoryMemoryRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import com.ipon.app.util.MerchantClassifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AddTransactionUiState(
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: TransactionCategory? = null,
    val merchantRaw: String = "",
    val note: String = "",
    val suggestedCategory: TransactionCategory? = null,
    /** True when [suggestedCategory] came from the user's own remembered history, not the generic keyword rules. */
    val suggestionIsFromMemory: Boolean = false,
    val amountError: String? = null,
    val savedTransactionId: String? = null,
    /** Null when creating a new transaction; set to the original transaction's ID when editing an existing one. */
    val editingTransactionId: String? = null,
    val deleted: Boolean = false
) {
    val isEditing: Boolean get() = editingTransactionId != null
}

class AddTransactionViewModel(
    private val repository: TransactionRepository,
    private val merchantClassifier: MerchantClassifier,
    private val categoryMemoryRepository: CategoryMemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    // Preserves the original occurredAtEpochMillis when editing -- editing
    // a transaction changes its amount/category/etc, not when it happened,
    // so this is kept out of the editable UI state entirely and just
    // carried through to save().
    private var editingOccurredAtEpochMillis: Long? = null

    // Cancels and replaces the in-flight memory lookup whenever the
    // merchant text or type changes again before the previous lookup
    // returned -- otherwise a slow lookup for an earlier keystroke could
    // resolve after a newer one and overwrite a more current suggestion.
    private var memoryLookupJob: Job? = null

    /**
     * Fetches the transaction by ID and loads it into the form. Called once
     * from AddTransactionScreen's LaunchedEffect when opened in edit mode.
     * Silently no-ops if the ID doesn't resolve to a real transaction
     * (e.g. it was deleted from another path) rather than crashing --
     * worst case the form just opens blank, which is recoverable by the
     * user backing out.
     */
    fun loadTransactionForEditing(transactionId: String) {
        viewModelScope.launch {
            val transaction = repository.getById(transactionId) ?: return@launch
            initializeForEdit(transaction)
        }
    }

    /**
     * Loads an existing transaction into the form for editing. Called once
     * from AddTransactionScreen when it's opened in edit mode (see the
     * `editingTransactionId` nav argument in IponNavHost). Suggestion state
     * is intentionally left empty here -- re-running classification against
     * a transaction the user already categorized themselves would be
     * pointless noise, not a helpful suggestion.
     */
    private fun initializeForEdit(transaction: Transaction) {
        editingOccurredAtEpochMillis = transaction.occurredAtEpochMillis
        _uiState.update {
            it.copy(
                amountInput = formatMinorUnitsAsInput(transaction.amount.minorUnits),
                type = transaction.type,
                selectedCategory = resolveCategory(transaction.category, transaction.type),
                merchantRaw = transaction.merchantRaw.orEmpty(),
                note = transaction.note.orEmpty(),
                editingTransactionId = transaction.id
            )
        }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amountInput = value, amountError = null) }
    }

    fun onTypeChanged(type: TransactionType) {
        // Expense and income use entirely separate category enums (see
        // Category.kt), so a category chosen under one type is meaningless
        // under the other -- clear it rather than carry over a mismatched
        // selection, and re-run classification against the new type since
        // the same merchant string can mean different things depending on
        // direction (e.g. "GCash" alone is ambiguous; the type disambiguates).
        val resuggested = merchantClassifier.classify(_uiState.value.merchantRaw, type)
        _uiState.update {
            it.copy(
                type = type,
                selectedCategory = resuggested,
                suggestedCategory = resuggested,
                suggestionIsFromMemory = false
            )
        }
        lookUpMemory(_uiState.value.merchantRaw, type)
    }

    fun onMerchantChanged(value: String) {
        // Section 3 / category memory: the keyword classifier gives an
        // instant synchronous suggestion as the user types (no DB hit, so
        // no lag), and a remembered correction -- if one exists for this
        // exact merchant+type -- overrides it a moment later once the
        // (cheap, but still suspend) memory lookup returns. The user's own
        // past choice always wins over the generic keyword rules; see
        // CategoryMemoryRepository's header comment.
        val suggestion = merchantClassifier.classify(value, _uiState.value.type)
        _uiState.update {
            it.copy(
                merchantRaw = value,
                suggestedCategory = suggestion,
                suggestionIsFromMemory = false,
                selectedCategory = it.selectedCategory ?: suggestion
            )
        }
        lookUpMemory(value, _uiState.value.type)
    }

    private fun lookUpMemory(merchantRaw: String, type: TransactionType) {
        memoryLookupJob?.cancel()
        if (merchantRaw.isBlank()) return
        memoryLookupJob = viewModelScope.launch {
            val remembered = categoryMemoryRepository.recall(merchantRaw, type)
            if (remembered != null) {
                _uiState.update { current ->
                    // Only override if the user hasn't since picked a
                    // different category themselves while this lookup was
                    // in flight.
                    val userHasntOverridden = current.selectedCategory == current.suggestedCategory ||
                            current.selectedCategory == null
                    current.copy(
                        suggestedCategory = remembered,
                        suggestionIsFromMemory = true,
                        selectedCategory = if (userHasntOverridden) remembered else current.selectedCategory
                    )
                }
            }
        }
    }

    fun onCategorySelected(category: TransactionCategory) {
        _uiState.update { it.copy(selectedCategory = category, suggestionIsFromMemory = false) }
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

        val category = state.selectedCategory ?: defaultOtherFor(state.type)
        val wasAutoCategorized = state.suggestedCategory != null && state.selectedCategory == state.suggestedCategory

        val transaction = Transaction(
            id = state.editingTransactionId ?: UUID.randomUUID().toString(),
            amount = amount,
            type = state.type,
            category = category.displayName,
            merchantRaw = state.merchantRaw.ifBlank { null },
            note = state.note.ifBlank { null },
            occurredAtEpochMillis = editingOccurredAtEpochMillis ?: System.currentTimeMillis(),
            isAutoCategorized = wasAutoCategorized
        )

        viewModelScope.launch {
            if (state.isEditing) {
                repository.update(transaction)
            } else {
                repository.add(transaction)
            }
            // Every save teaches the memory, regardless of whether the
            // category came from a suggestion or the user picked it fresh
            // -- see CategoryMemoryRepository.remember()'s header comment
            // for why this is a "vote" rather than a blind overwrite.
            if (state.merchantRaw.isNotBlank()) {
                categoryMemoryRepository.remember(state.merchantRaw, state.type, category)
            }
            _uiState.update { it.copy(savedTransactionId = transaction.id) }
        }
    }

    /**
     * Deletes the transaction currently being edited. No-op (and should
     * never be reachable from the UI) if this ViewModel isn't in edit mode
     * -- see AddTransactionScreen, which only shows a Delete action when
     * `uiState.isEditing` is true.
     */
    fun delete() {
        val state = _uiState.value
        val id = state.editingTransactionId ?: return
        val amount = Money.parse(state.amountInput) ?: Money.ZERO
        val category = state.selectedCategory ?: defaultOtherFor(state.type)

        val transaction = Transaction(
            id = id,
            amount = amount,
            type = state.type,
            category = category.displayName,
            merchantRaw = state.merchantRaw.ifBlank { null },
            note = state.note.ifBlank { null },
            occurredAtEpochMillis = editingOccurredAtEpochMillis ?: System.currentTimeMillis(),
            isAutoCategorized = false
        )

        viewModelScope.launch {
            repository.delete(transaction)
            _uiState.update { it.copy(deleted = true) }
        }
    }

    private fun defaultOtherFor(type: TransactionType): TransactionCategory = when (type) {
        TransactionType.EXPENSE -> ExpenseCategory.OTHER
        TransactionType.INCOME -> IncomeCategory.OTHER
    }

    /** Renders 15050 minor units back into the "150.50" form the amount TextField expects. */
    private fun formatMinorUnitsAsInput(minorUnits: Long): String {
        val sign = if (minorUnits < 0) "-" else ""
        val absValue = kotlin.math.abs(minorUnits)
        val pesos = absValue / 100
        val centavos = absValue % 100
        return "$sign$pesos.${centavos.toString().padString(2, '0')}"
    }

    private fun String.padString(length: Int, padChar: Char): String {
        if (this.length >= length) return this
        val sb = StringBuilder(length)
        for (i in 0 until length - this.length) {
            sb.append(padChar)
        }
        sb.append(this)
        return sb.toString()
    }
}
