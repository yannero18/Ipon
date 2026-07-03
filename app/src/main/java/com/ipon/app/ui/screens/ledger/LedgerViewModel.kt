package com.ipon.app.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.data.model.estimateDaysOfRunway
import com.ipon.app.data.repository.EnvelopeRepository
import com.ipon.app.data.repository.GoalRepository
import com.ipon.app.data.repository.PeriodSummary
import com.ipon.app.data.repository.RecurringTemplateRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

sealed interface LedgerUiState {
    val isLoading: Boolean
    val transactions: List<Transaction>
    val summary: PeriodSummary
    val estimatedDaysOfRunway: Int?
    val totalSavingsBalance: Money
    val activeGoals: List<GoalProgress>
    val monthlyContributionsSum: Money
    val envelopes: List<EnvelopeProgress>
    val availableBalance: Money
    val remainingBudget: Money
    val totalEnvelopeCaps: Money
    val dueTemplates: List<RecurringTemplate>

    object Loading : LedgerUiState {
        override val isLoading: Boolean = true
        override val transactions: List<Transaction> = emptyList()
        override val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO)
        override val estimatedDaysOfRunway: Int? = null
        override val totalSavingsBalance: Money = Money.ZERO
        override val activeGoals: List<GoalProgress> = emptyList()
        override val monthlyContributionsSum: Money = Money.ZERO
        override val envelopes: List<EnvelopeProgress> = emptyList()
        override val availableBalance: Money = Money.ZERO
        override val remainingBudget: Money = Money.ZERO
        override val totalEnvelopeCaps: Money = Money.ZERO
        override val dueTemplates: List<RecurringTemplate> = emptyList()
    }

    data class Success(
        override val transactions: List<Transaction> = emptyList(),
        override val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO),
        override val estimatedDaysOfRunway: Int? = null,
        override val totalSavingsBalance: Money = Money.ZERO,
        override val activeGoals: List<GoalProgress> = emptyList(),
        override val monthlyContributionsSum: Money = Money.ZERO,
        override val envelopes: List<EnvelopeProgress> = emptyList(),
        override val availableBalance: Money = Money.ZERO,
        override val remainingBudget: Money = Money.ZERO,
        override val totalEnvelopeCaps: Money = Money.ZERO,
        override val dueTemplates: List<RecurringTemplate> = emptyList()
    ) : LedgerUiState {
        override val isLoading: Boolean = false
    }

    data class Empty(
        override val totalSavingsBalance: Money = Money.ZERO,
        override val activeGoals: List<GoalProgress> = emptyList(),
        override val monthlyContributionsSum: Money = Money.ZERO,
        override val dueTemplates: List<RecurringTemplate> = emptyList()
    ) : LedgerUiState {
        override val isLoading: Boolean = false
        override val transactions: List<Transaction> = emptyList()
        override val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO)
        override val estimatedDaysOfRunway: Int? = null
        override val envelopes: List<EnvelopeProgress> = emptyList()
        override val availableBalance: Money = Money.ZERO
        override val remainingBudget: Money = Money.ZERO
        override val totalEnvelopeCaps: Money = Money.ZERO
    }
}

class LedgerViewModel(
    private val transactionRepository: TransactionRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val goalRepository: GoalRepository,
    private val recurringTemplateRepository: RecurringTemplateRepository
) : ViewModel() {

    private val currentPeriod = currentYearMonth()
    private val monthRange = currentMonthRangeMillis()

    val uiState: StateFlow<LedgerUiState> = combine(
        transactionRepository.observeBetween(monthRange.first, monthRange.second),
        transactionRepository.observeSummaryBetween(monthRange.first, monthRange.second),
        envelopeRepository.observeProgressForPeriod(currentPeriod, monthRange.first, monthRange.second),
        goalRepository.observeProgress(),
        goalRepository.observeAllContributions()
    ) { transactions, summary, envelopes, goals, contributions ->
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val averageDailySpend = if (dayOfMonth > 0) {
            Money.ofMinorUnits(summary.expense.minorUnits / dayOfMonth)
        } else {
            Money.ZERO
        }

        // Calculate total savings balance
        val totalSavings = Money.ofMinorUnits(goals.sumOf { it.saved.minorUnits })

        // Calculate monthly contributions sum (contributions made in this month's range)
        val monthlyContributions = Money.ofMinorUnits(
            contributions
                .filter { it.contributedAtEpochMillis in monthRange.first..monthRange.second }
                .sumOf { it.amount.minorUnits }
        )

        // Calculate available balance (unallocated = income - expense - sum of envelope caps)
        val totalEnvelopeCapsMinor = envelopes.sumOf { it.cap.minorUnits }
        val available = Money.ofMinorUnits(
            (summary.income.minorUnits - summary.expense.minorUnits - totalEnvelopeCapsMinor).coerceAtLeast(0L)
        )
        
        val remainingBudgetMinor = totalEnvelopeCapsMinor - summary.expense.minorUnits
        val remainingBudget = Money.ofMinorUnits(remainingBudgetMinor)
        val totalEnvelopeCaps = Money.ofMinorUnits(totalEnvelopeCapsMinor)

        if (transactions.isEmpty() && envelopes.isEmpty()) {
            LedgerUiState.Empty(
                totalSavingsBalance = totalSavings,
                activeGoals = goals,
                monthlyContributionsSum = monthlyContributions
            )
        } else {
            LedgerUiState.Success(
                transactions = transactions,
                summary = summary,
                estimatedDaysOfRunway = if (dayOfMonth >= 3) {
                    estimateDaysOfRunway(summary.net, averageDailySpend)
                } else {
                    null
                },
                totalSavingsBalance = totalSavings,
                activeGoals = goals,
                monthlyContributionsSum = monthlyContributions,
                envelopes = envelopes,
                availableBalance = available,
                remainingBudget = remainingBudget,
                totalEnvelopeCaps = totalEnvelopeCaps
            )
        }
    }.combine(recurringTemplateRepository.observeDue()) { state, due ->
        when (state) {
            is LedgerUiState.Loading -> state
            is LedgerUiState.Empty -> state.copy(dueTemplates = due)
            is LedgerUiState.Success -> state.copy(dueTemplates = due)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerUiState.Loading
    )

    fun autoConfirmAllDueTemplates() {
        viewModelScope.launch {
            val due = uiState.value.dueTemplates
            due.forEach { template ->
                recurringTemplateRepository.confirmIntoTransaction(template)
            }
        }
    }

    fun setEnvelopeCap(category: ExpenseCategory, amount: Money) {
        viewModelScope.launch {
            envelopeRepository.setCap(category, currentPeriod, amount)
        }
    }

    fun removeEnvelopeCap(category: ExpenseCategory) {
        viewModelScope.launch {
            envelopeRepository.removeCap(category, currentPeriod)
        }
    }

    fun addQuickTransaction(type: TransactionType, amount: Money, categoryName: String, label: String) {
        viewModelScope.launch {
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                amount = amount,
                type = type,
                category = categoryName,
                merchantRaw = null,
                note = label,
                occurredAtEpochMillis = System.currentTimeMillis(),
                isAutoCategorized = false
            )
            transactionRepository.add(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }

    fun addQuickGoalContribution(goalId: String, amount: Money) {
        viewModelScope.launch {
            goalRepository.addContribution(goalId, amount, note = "Quick deposit via Dashboard")
        }
    }

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
        val end = calendar.timeInMillis

        return start to end
    }
}
