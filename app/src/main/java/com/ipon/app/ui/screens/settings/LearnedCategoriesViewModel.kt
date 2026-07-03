package com.ipon.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.repository.CategoryMemoryRepository
import com.ipon.app.data.repository.LearnedCategoryMemory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LearnedCategoriesViewModel(private val repository: CategoryMemoryRepository) : ViewModel() {

    val memories: StateFlow<List<LearnedCategoryMemory>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun forget(memory: LearnedCategoryMemory) {
        viewModelScope.launch {
            repository.forget(memory)
        }
    }
}
