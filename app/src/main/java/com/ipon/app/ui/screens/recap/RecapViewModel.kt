package com.ipon.app.ui.screens.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.YearRecap
import com.ipon.app.data.repository.RecapRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class RecapViewModel(repository: RecapRepository) : ViewModel() {

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val recap: StateFlow<YearRecap> = repository.observeRecapForYear(currentYear)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            YearRecap(
                year = currentYear,
                totalIncome = Money.ZERO,
                totalExpense = Money.ZERO,
                transactionCount = 0,
                topCategories = emptyList(),
                busiestMonth = null,
                moodCounts = emptyMap(),
                goalsCompletedThisYear = 0,
                totalSavedTowardGoals = Money.ZERO
            )
        )
}
