package com.ipon.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.CategoryTrend
import com.ipon.app.data.model.SavingsSuggestion
import com.ipon.app.data.model.generateSavingsSuggestions
import com.ipon.app.data.model.CategorySlice
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.Report
import com.ipon.app.data.model.Transaction
import com.ipon.app.data.repository.EnvelopeRepository
import com.ipon.app.data.repository.PeriodSummary
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.data.repository.RecurringTemplateRepository
import com.ipon.app.data.repository.GoalRepository
import com.ipon.app.data.repository.DailyReflectionRepository
import com.ipon.app.data.model.DailyReflection
import com.ipon.app.data.repository.ReportRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class InsightsUiState(
    val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val trends: List<CategoryTrend> = emptyList(),
    val suggestions: List<SavingsSuggestion> = emptyList(),
    val envelopes: List<EnvelopeProgress> = emptyList(),
    val reflections: List<DailyReflection> = emptyList(),
    val monthTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

class InsightsViewModel(
    private val transactionRepository: TransactionRepository,
    private val envelopeRepository: EnvelopeRepository,
    private val recurringTemplateRepository: RecurringTemplateRepository,
    private val goalRepository: GoalRepository,
    private val reportRepository: ReportRepository,
    private val dailyReflectionRepository: DailyReflectionRepository
) : ViewModel() {

    private val thisMonthRange = monthRangeMillis(monthOffset = 0)
    private val lastMonthRange = monthRangeMillis(monthOffset = -1)
    private val currentPeriodKey = currentYearMonth()

    val uiState: StateFlow<InsightsUiState> = combine(
        transactionRepository.observeSummaryBetween(thisMonthRange.first, thisMonthRange.second),
        transactionRepository.observeCategoryBreakdownBetween(thisMonthRange.first, thisMonthRange.second),
        transactionRepository.observeCategoryTrends(
            thisMonthStart = thisMonthRange.first,
            thisMonthEnd = thisMonthRange.second,
            lastMonthStart = lastMonthRange.first,
            lastMonthEnd = lastMonthRange.second
        ),
        envelopeRepository.observeProgressForPeriod(
            periodYearMonth = currentPeriodKey,
            startEpochMillis = thisMonthRange.first,
            endEpochMillis = thisMonthRange.second
        ),
        dailyReflectionRepository.observeRecent(400),
        transactionRepository.observeBetween(thisMonthRange.first, thisMonthRange.second)
    ) { array ->
        val summary = array[0] as PeriodSummary
        val breakdown = array[1] as List<CategorySlice>
        val trends = array[2] as List<CategoryTrend>
        val envelopeProgress = array[3] as List<EnvelopeProgress>
        val reflections = array[4] as List<DailyReflection>
        val monthTransactions = array[5] as List<Transaction>

        val filteredTrends = trends
            .filter { !it.thisMonth.isZero || !it.lastMonth.isZero }
            .sortedByDescending { it.thisMonth.minorUnits }

        val reflectionsThisMonth = reflections.filter { it.dayKey.startsWith(currentPeriodKey) }

        InsightsUiState(
            summary = summary,
            categoryBreakdown = breakdown,
            trends = filteredTrends,
            suggestions = generateSavingsSuggestions(
                trends = filteredTrends,
                envelopeProgress = envelopeProgress,
                totalIncome = summary.income,
                totalExpense = summary.expense
            ),
            envelopes = envelopeProgress,
            reflections = reflectionsThisMonth,
            monthTransactions = monthTransactions,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    // Flow of saved reports
    val reports: StateFlow<List<Report>> = reportRepository.observeAllReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Engine 1: Generate structuredspending report and save as local Report entity.
     */
    suspend fun generateMonthlyReport(): Report {
        val currentState = uiState.value
        val transactions = transactionRepository.observeBetween(thisMonthRange.first, thisMonthRange.second).first()
        
        return reportRepository.generateAndSaveMonthlyReport(
            periodYearMonth = currentPeriodKey,
            income = currentState.summary.income,
            expense = currentState.summary.expense,
            transactions = transactions,
            envelopes = currentState.envelopes
        )
    }

    /**
     * Engine 2: Auto-process recurring deposits/fixed bills due today or past due.
     */
    suspend fun processDueRecurringTransactions(): Int {
        val today = Calendar.getInstance()
        val dueTemplates = recurringTemplateRepository.observeDue(today).first()
        var count = 0
        dueTemplates.forEach { template ->
            recurringTemplateRepository.confirmIntoTransaction(
                template = template,
                calendar = today,
                occurredAtEpochMillis = System.currentTimeMillis()
            )
            count++
        }
        return count
    }

    /**
     * Engine 3: Sweep excess/remaining unspent envelope budgets directly to active goals.
     * Logs offsetting ledger entries to close the envelopes and logs contributions to goals.
     */
    suspend fun sweepExcessFundsToGoals(): List<String> {
        val currentState = uiState.value
        val activeGoals = goalRepository.observeProgress().first().filter { !it.goal.isArchived }
        if (activeGoals.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<String>()
        val sweptEnvelopes = currentState.envelopes
            .filter { !it.cap.isZero && it.remaining.isPositive }
            .sortedByDescending { it.priority }

        sweptEnvelopes.forEach { envelope ->
            val remaining = envelope.remaining
            
            // Add EXPENSE transaction under the envelope's category to close the budget and maintain ledger balance
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                amount = remaining,
                type = TransactionType.EXPENSE,
                category = envelope.category.displayName,
                merchantRaw = null,
                note = "Envelope Sweep to Savings",
                occurredAtEpochMillis = System.currentTimeMillis(),
                isAutoCategorized = false
            )
            transactionRepository.add(tx)

            // Distribute the swept amount equally among active goals
            val partMinor = remaining.minorUnits / activeGoals.size
            if (partMinor > 0L) {
                val partMoney = Money.ofMinorUnits(partMinor)
                activeGoals.forEach { goalProgress ->
                    goalRepository.addContribution(
                        goalId = goalProgress.goal.id,
                        amount = partMoney,
                        note = "Sweep: Remaining from ${envelope.category.displayName} (Priority: ${envelope.priority})"
                    )
                }
            }

            val priorityLabel = when (envelope.priority) {
                3 -> "High"
                1 -> "Low"
                else -> "Medium"
            }
            results.add("Swept Php $remaining from ${envelope.category.displayName} ($priorityLabel priority) to ${activeGoals.size} active goals.")
        }

        return results
    }

    suspend fun deleteReport(id: String) {
        reportRepository.deleteReportById(id)
    }

    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun monthRangeMillis(monthOffset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
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
