package com.ipon.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.model.Goal
import com.ipon.app.data.model.GoalContribution
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.data.repository.GoalRepository
import com.ipon.app.util.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalProgress> = emptyList(),
    val isLoading: Boolean = true
)

class GoalsViewModel(private val repository: GoalRepository) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = repository.observeProgress()
        .map { progress -> GoalsUiState(goals = progress, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    // UPDATED to accept imageUri
    fun createGoal(label: String, target: Money, emoji: String, deadline: String?, imageUri: String? = null) {
        viewModelScope.launch {
            repository.createGoal(label, target, emoji, deadline, imageUri)
        }
    }

    fun addContribution(goal: Goal, amount: Money, note: String?) {
        viewModelScope.launch {
            repository.addContribution(goal.id, amount, note)
        }
    }

    fun archiveGoal(goal: Goal) {
        viewModelScope.launch {
            repository.archiveGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    /** Newest-first contribution history for one goal, for GoalDetailScreen. */
    fun contributionsForGoal(goalId: String): Flow<List<GoalContribution>> =
        repository.observeContributionsForGoal(goalId)
}