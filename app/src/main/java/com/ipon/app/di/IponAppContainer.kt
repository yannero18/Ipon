package com.ipon.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ipon.app.data.local.IponDatabase
import com.ipon.app.data.repository.AppDataRepository
import com.ipon.app.data.repository.BudgetPlanRepository
import com.ipon.app.data.repository.CategoryMemoryRepository
import com.ipon.app.data.repository.DailyReflectionRepository
import com.ipon.app.data.repository.DebtRepository
import com.ipon.app.data.repository.EnvelopeRepository
import com.ipon.app.data.repository.ExportRepository
import com.ipon.app.data.repository.GoalRepository
import com.ipon.app.data.repository.RecapRepository
import com.ipon.app.data.repository.RecurringTemplateRepository
import com.ipon.app.data.repository.ReportRepository
import com.ipon.app.data.repository.TransactionRepository
import com.ipon.app.ui.screens.addtransaction.AddTransactionViewModel
import com.ipon.app.ui.screens.budgetplan.BudgetPlanViewModel
import com.ipon.app.ui.screens.debts.DebtsViewModel
import com.ipon.app.ui.screens.envelopes.EnvelopesViewModel
import com.ipon.app.ui.screens.goals.GoalsViewModel
import com.ipon.app.ui.screens.insights.InsightsViewModel
import com.ipon.app.ui.screens.ledger.LedgerViewModel
import com.ipon.app.ui.screens.recap.RecapViewModel
import com.ipon.app.ui.screens.recurring.RecurringViewModel
import com.ipon.app.ui.screens.reflection.ReflectionViewModel
import com.ipon.app.ui.screens.settings.LearnedCategoriesViewModel
import com.ipon.app.ui.screens.settings.SettingsViewModel
import com.ipon.app.util.HapticFeedbackManager
import com.ipon.app.util.KeywordRuleMerchantClassifier
import com.ipon.app.util.OnboardingPreferences

/**
 * Manual dependency container for the prototype. A real app would likely
 * reach for Hilt here, but a single object graph keeps this skeleton
 * dependency-free and easy to read end to end without generated code.
 */
class IponAppContainer(context: Context) {

    private val database = IponDatabase.getInstance(context)
    val repository = TransactionRepository(database.transactionDao())
    val envelopeRepository = EnvelopeRepository(database.envelopeDao(), database.transactionDao())
    val recurringTemplateRepository = RecurringTemplateRepository(database, database.recurringTemplateDao())
    val goalRepository = GoalRepository(database.goalDao())
    val dailyReflectionRepository = DailyReflectionRepository(database.dailyReflectionDao(), database.transactionDao())
    val appDataRepository = AppDataRepository(database)
    val categoryMemoryRepository = CategoryMemoryRepository(database.merchantCategoryMemoryDao())
    val exportRepository = ExportRepository(context, database)
    val recapRepository = RecapRepository(repository, goalRepository, dailyReflectionRepository)
    val budgetPlanRepository = BudgetPlanRepository(repository, envelopeRepository, recurringTemplateRepository, goalRepository)
    val debtRepository = DebtRepository(database.debtDao())
    val reportRepository = ReportRepository(database.reportDao())
    val merchantClassifier = KeywordRuleMerchantClassifier()
    val hapticFeedback = HapticFeedbackManager(context)
    val onboardingPreferences = OnboardingPreferences(context)
}

class IponViewModelFactory(private val container: IponAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LedgerViewModel::class.java -> LedgerViewModel(
            container.repository,
            container.envelopeRepository,
            container.goalRepository,
            container.recurringTemplateRepository,
            container.onboardingPreferences
        ) as T
        AddTransactionViewModel::class.java -> AddTransactionViewModel(
            container.repository,
            container.merchantClassifier,
            container.categoryMemoryRepository
        ) as T
        InsightsViewModel::class.java -> InsightsViewModel(
            container.repository,
            container.envelopeRepository,
            container.recurringTemplateRepository,
            container.goalRepository,
            container.reportRepository,
            container.dailyReflectionRepository
        ) as T
        EnvelopesViewModel::class.java -> EnvelopesViewModel(container.envelopeRepository, container.repository) as T
        RecurringViewModel::class.java -> RecurringViewModel(container.recurringTemplateRepository, container.repository) as T
        GoalsViewModel::class.java -> GoalsViewModel(container.goalRepository) as T
        ReflectionViewModel::class.java -> ReflectionViewModel(container.dailyReflectionRepository) as T
        SettingsViewModel::class.java -> SettingsViewModel(container.appDataRepository, container.exportRepository) as T
        LearnedCategoriesViewModel::class.java -> LearnedCategoriesViewModel(container.categoryMemoryRepository) as T
        RecapViewModel::class.java -> RecapViewModel(container.recapRepository) as T
        BudgetPlanViewModel::class.java -> BudgetPlanViewModel(container.budgetPlanRepository) as T
        DebtsViewModel::class.java -> DebtsViewModel(container.debtRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
