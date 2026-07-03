package com.ipon.app.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.RecurringPatternSuggestion
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.data.model.detectRecurringPatterns
import com.ipon.app.data.repository.RecurringTemplateRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class RecurringUiState(
    val dueTemplates: List<RecurringTemplate> = emptyList(),
    val allTemplates: List<RecurringTemplate> = emptyList(),
    val detectedPatterns: List<RecurringPatternSuggestion> = emptyList(),
    val isLoading: Boolean = true
)

class RecurringViewModel(
    private val repository: RecurringTemplateRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // In-memory only -- dismissing a detected pattern suggestion is a
    // "not now," not a permanent decision worth a database row. If it's
    // still a real pattern next app launch, it'll surface again, which is
    // the right behavior for a low-stakes nudge like this.
    private val dismissedMerchantKeys = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<RecurringUiState> = combine(
        repository.observeDue(),
        repository.observeAll(),
        recentTransactionHistory(),
        repository.observeExistingMerchantKeys(),
        dismissedMerchantKeys
    ) { due, all, recentTransactions, existingKeys, dismissed ->
        val patterns = detectRecurringPatterns(recentTransactions, existingKeys)
            .filter { it.merchantKey !in dismissed }

        RecurringUiState(
            dueTemplates = due,
            allTemplates = all,
            detectedPatterns = patterns,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringUiState())

    /** Last ~120 days is enough history to catch a monthly pattern at least 3 times over. */
    private fun recentTransactionHistory() = run {
        val end = System.currentTimeMillis()
        val start = end - (120L * 24 * 60 * 60 * 1000)
        transactionRepository.observeBetween(start, end)
    }

    fun confirm(template: RecurringTemplate) {
        viewModelScope.launch {
            repository.confirmIntoTransaction(template)
        }
    }

    fun togglePaused(template: RecurringTemplate) {
        viewModelScope.launch {
            repository.setPaused(template, paused = !template.isPaused)
        }
    }

    fun delete(template: RecurringTemplate) {
        viewModelScope.launch {
            repository.delete(template)
        }
    }

    fun dismissPattern(suggestion: RecurringPatternSuggestion) {
        dismissedMerchantKeys.update { it + suggestion.merchantKey }
    }

    fun createTemplateFromPattern(suggestion: RecurringPatternSuggestion, dayOfPeriod: Int) {
        createTemplate(
            label = suggestion.merchantRawExample,
            amount = suggestion.averageAmount,
            type = suggestion.type,
            category = suggestion.category,
            merchantRaw = suggestion.merchantRawExample,
            frequency = suggestion.suggestedFrequency,
            dayOfPeriod = dayOfPeriod
        )
        dismissPattern(suggestion)
    }

    fun createTemplate(
        label: String,
        amount: Money,
        type: TransactionType,
        category: TransactionCategory,
        merchantRaw: String?,
        frequency: RecurrenceFrequency,
        dayOfPeriod: Int
    ) {
        viewModelScope.launch {
            repository.create(
                RecurringTemplate(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    amount = amount,
                    type = type,
                    category = category,
                    merchantRaw = merchantRaw,
                    frequency = frequency,
                    dayOfPeriod = dayOfPeriod,
                    lastConfirmedPeriodKey = null,
                    isPaused = false
                )
            )
        }
    }
}
