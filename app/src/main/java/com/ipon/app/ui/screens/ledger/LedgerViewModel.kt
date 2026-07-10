package com.ipon.app.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.data.model.estimateDaysOfRunway
import com.ipon.app.data.repository.CategoryMemoryRepository
import com.ipon.app.data.repository.EnvelopeRepository
import com.ipon.app.data.repository.GoalRepository
import com.ipon.app.data.repository.PeriodSummary
import com.ipon.app.data.repository.RecurringTemplateRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.util.MerchantClassifier
import com.ipon.app.util.Money
import com.ipon.app.util.OnboardingPreferences
import com.ipon.app.util.parseShorthandTransaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

sealed interface LedgerUiState {
    val isLoading: Boolean
    val accountName: String
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
    
    // NEW PAYDAY VARIABLES
    val daysUntilPayday: Int?
    val safeDailySpend: Money
    val currentPaydays: List<Int>

    object Loading : LedgerUiState {
        override val isLoading = true
        override val accountName = "Yannero"
        override val transactions = emptyList<Transaction>()
        override val summary = PeriodSummary(Money.ZERO, Money.ZERO)
        override val estimatedDaysOfRunway = null
        override val totalSavingsBalance = Money.ZERO
        override val activeGoals = emptyList<GoalProgress>()
        override val monthlyContributionsSum = Money.ZERO
        override val envelopes = emptyList<EnvelopeProgress>()
        override val availableBalance = Money.ZERO
        override val remainingBudget = Money.ZERO
        override val totalEnvelopeCaps = Money.ZERO
        override val dueTemplates = emptyList<RecurringTemplate>()
        override val daysUntilPayday = null
        override val safeDailySpend = Money.ZERO
        override val currentPaydays = emptyList<Int>()
    }

    data class Success(
        override val accountName: String = "Yannero",
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
        override val dueTemplates: List<RecurringTemplate> = emptyList(),
        override val daysUntilPayday: Int? = null,
        override val safeDailySpend: Money = Money.ZERO,
        override val currentPaydays: List<Int> = emptyList()
    ) : LedgerUiState {
        override val isLoading = false
    }

    data class Empty(
        override val accountName: String = "Yannero",
        override val totalSavingsBalance: Money = Money.ZERO,
        override val activeGoals: List<GoalProgress> = emptyList(),
        override val monthlyContributionsSum: Money = Money.ZERO,
        override val dueTemplates: List<RecurringTemplate> = emptyList(),
        override val daysUntilPayday: Int? = null,
        override val safeDailySpend: Money = Money.ZERO,
        override val currentPaydays: List<Int> = emptyList(),
        override val availableBalance: Money = Money.ZERO,
        override val remainingBudget: Money = Money.ZERO,
        override val totalEnvelopeCaps: Money = Money.ZERO
    ) : LedgerUiState {
        override val isLoading = false
        override val transactions = emptyList<Transaction>()
        override val summary = PeriodSummary(Money.ZERO, Money.ZERO)
        override val estimatedDaysOfRunway = null
        override val envelopes = emptyList<EnvelopeProgress>()
    }
}

private data class TempLedgerData(
    val name: String, val transactions: List<Transaction>, val summary: PeriodSummary,
    val dayOfMonth: Int, val maxDays: Int, val averageDailySpend: Money,
    val totalSavings: Money, val monthlyContributions: Money, val envelopes: List<EnvelopeProgress>,
    val available: Money, val remainingBudget: Money, val totalEnvelopeCaps: Money, val goals: List<GoalProgress>
)

/**
 * State for the Tarsi-style one-line quick-add bar at the top of the
 * Ledger ("250 Grab", "+500 sweldo"). Parsing and category inference both
 * happen entirely on-device -- see [parseShorthandTransaction] and
 * [MerchantClassifier] -- nothing here ever leaves the phone.
 */
data class QuickAddUiState(
    val text: String = "",
    val amountInput: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val merchantText: String? = null,
    val category: TransactionCategory? = null,
    val categoryIsFromMemory: Boolean = false
) {
    /** True once there's a usable amount to submit -- gates the submit button/action. */
    val isValid: Boolean get() = !amountInput.isNullOrBlank() && Money.parse(amountInput).let { it != null && !it.isZero }
}

class LedgerViewModel(
    private val transactionRepository: TransactionRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val goalRepository: GoalRepository,
    private val recurringTemplateRepository: RecurringTemplateRepository,
    private val onboardingPreferences: OnboardingPreferences,
    private val merchantClassifier: MerchantClassifier,
    private val categoryMemoryRepository: CategoryMemoryRepository
) : ViewModel() {

    private val currentPeriod = currentYearMonth()
    private val monthRange = currentMonthRangeMillis()
    private val paydaysFlow = MutableStateFlow(onboardingPreferences.getPaydays())

    private val _quickAddState = MutableStateFlow(QuickAddUiState())
    val quickAddState: StateFlow<QuickAddUiState> = _quickAddState.asStateFlow()
    private var quickAddMemoryLookupJob: Job? = null

    private val ledgerDataFlow = combine(
        transactionRepository.observeBetween(monthRange.first, monthRange.second),
        transactionRepository.observeSummaryBetween(monthRange.first, monthRange.second),
        envelopeRepository.observeProgressForPeriod(currentPeriod, monthRange.first, monthRange.second)
    ) { transactions, summary, envelopes ->
        Triple(transactions, summary, envelopes)
    }

    private val goalsDataFlow = combine(
        goalRepository.observeProgress(),
        goalRepository.observeAllContributions()
    ) { goals, contributions ->
        Pair(goals, contributions)
    }

    val uiState: StateFlow<LedgerUiState> = combine(
        ledgerDataFlow,
        goalsDataFlow,
        recurringTemplateRepository.observeDue(),
        paydaysFlow
    ) { (transactions, summary, envelopes), (goals, contributions), dueTemplates, paydays ->
        val name = onboardingPreferences.getAccountName()
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val maxDays = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        val averageDailySpend = if (dayOfMonth > 0) {
            Money.ofMinorUnits(summary.expense.minorUnits / dayOfMonth)
        } else {
            Money.ZERO
        }

        val totalSavings = Money.ofMinorUnits(goals.sumOf { it.saved.minorUnits })

        val monthlyContributions = Money.ofMinorUnits(
            contributions
                .filter { it.contributedAtEpochMillis in monthRange.first..monthRange.second }
                .sumOf { it.amount.minorUnits }
        )

        val totalEnvelopeCapsMinor = envelopes.sumOf { it.cap.minorUnits }
        val available = Money.ofMinorUnits(
            summary.income.minorUnits - summary.expense.minorUnits
        )
        
        val remainingBudgetMinor = totalEnvelopeCapsMinor - summary.expense.minorUnits
        val remainingBudget = Money.ofMinorUnits(remainingBudgetMinor)
        val totalEnvelopeCaps = Money.ofMinorUnits(totalEnvelopeCapsMinor)
        
        val daysUntilPayday = calculateDaysUntilPayday(paydays, dayOfMonth, maxDays)
        val safeDailyMinor = if (daysUntilPayday != null && daysUntilPayday > 0 && available.minorUnits > 0) {
            available.minorUnits / daysUntilPayday
        } else if (daysUntilPayday == 0 && available.minorUnits > 0) {
            available.minorUnits 
        } else 0L
        val safeDailySpend = Money.ofMinorUnits(safeDailyMinor)

        if (transactions.isEmpty() && envelopes.isEmpty()) {
            LedgerUiState.Empty(
                accountName = name,
                totalSavingsBalance = totalSavings,
                activeGoals = goals,
                monthlyContributionsSum = monthlyContributions,
                dueTemplates = dueTemplates,
                currentPaydays = paydays,
                daysUntilPayday = daysUntilPayday,
                safeDailySpend = safeDailySpend
            )
        } else {
            LedgerUiState.Success(
                accountName = name,
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
                totalEnvelopeCaps = totalEnvelopeCaps,
                dueTemplates = dueTemplates,
                currentPaydays = paydays,
                daysUntilPayday = daysUntilPayday,
                safeDailySpend = safeDailySpend
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerUiState.Loading
    )

    private fun calculateDaysUntilPayday(paydays: List<Int>, currentDay: Int, maxDays: Int): Int? {
        if (paydays.isEmpty()) return null
        val validPaydays = paydays.map { it.coerceAtMost(maxDays) }.sorted()
        val nextPaydayThisMonth = validPaydays.firstOrNull { it >= currentDay }
        
        return if (nextPaydayThisMonth != null) {
            nextPaydayThisMonth - currentDay
        } else {
            val nextMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
            val nextMonthMax = nextMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstPaydayNextMonth = paydays.map { it.coerceAtMost(nextMonthMax) }.sorted().firstOrNull() ?: 1
            (maxDays - currentDay) + firstPaydayNextMonth
        }
    }

    fun updatePaydays(paydays: List<Int>) {
        onboardingPreferences.savePaydays(paydays)
        paydaysFlow.value = paydays
    }

    /**
     * Called on every keystroke in the quick-add bar. Splitting the text
     * and picking a type is synchronous and instant (see
     * [parseShorthandTransaction]); the keyword classifier's guess is also
     * synchronous, so it can render in the same frame. The remembered
     * per-merchant category (if any) arrives a moment later from
     * [lookUpQuickAddMemory], same two-step pattern as
     * AddTransactionViewModel.onMerchantChanged -- the user's own past
     * correction always wins over the generic keyword rules once it loads.
     */
    fun onQuickAddTextChanged(text: String) {
        val parsed = parseShorthandTransaction(text)
        val suggestion = parsed?.merchantText
            ?.takeIf { it.isNotBlank() }
            ?.let { merchantClassifier.classify(it, parsed.type) }

        _quickAddState.value = QuickAddUiState(
            text = text,
            amountInput = parsed?.amountInput,
            type = parsed?.type ?: TransactionType.EXPENSE,
            merchantText = parsed?.merchantText,
            category = suggestion,
            categoryIsFromMemory = false
        )

        quickAddMemoryLookupJob?.cancel()
        val merchantText = parsed?.merchantText
        if (!merchantText.isNullOrBlank()) {
            quickAddMemoryLookupJob = viewModelScope.launch {
                val remembered = categoryMemoryRepository.recall(merchantText, parsed.type)
                if (remembered != null) {
                    _quickAddState.update { current ->
                        // Only apply if the text hasn't changed underneath this lookup.
                        if (current.merchantText == merchantText) {
                            current.copy(category = remembered, categoryIsFromMemory = true)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    /**
     * Commits the current quick-add bar contents as a real transaction --
     * same direct, deliberate write as tapping Save on the full Add
     * Transaction screen, just with less typing. Clears the bar back to
     * empty on success so it's ready for the next line.
     */
    fun submitQuickAdd() {
        val state = _quickAddState.value
        val amount = state.amountInput?.let { Money.parse(it) } ?: return
        if (amount.isZero) return

        val category = state.category ?: when (state.type) {
            TransactionType.EXPENSE -> ExpenseCategory.OTHER
            TransactionType.INCOME -> IncomeCategory.OTHER
        }
        val merchantText = state.merchantText?.ifBlank { null }

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = state.type,
            category = category.displayName,
            merchantRaw = merchantText,
            note = null,
            occurredAtEpochMillis = System.currentTimeMillis(),
            isAutoCategorized = state.category != null
        )

        viewModelScope.launch {
            transactionRepository.add(transaction)
            if (merchantText != null) {
                categoryMemoryRepository.remember(merchantText, state.type, category)
            }
        }

        quickAddMemoryLookupJob?.cancel()
        _quickAddState.value = QuickAddUiState()
    }

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