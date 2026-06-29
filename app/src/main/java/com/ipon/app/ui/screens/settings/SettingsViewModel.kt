package com.ipon.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.repository.AppDataRepository
import com.ipon.app.data.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ExportResult {
    data class Success(val filename: String) : ExportResult
    data object Unsupported : ExportResult
    data object Failed : ExportResult
}

class SettingsViewModel(
    private val appDataRepository: AppDataRepository,
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _dataCleared = MutableStateFlow(false)
    val dataCleared: StateFlow<Boolean> = _dataCleared.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _isExporting.value = true
            _exportResult.value = try {
                val filename = withContext(Dispatchers.IO) {
                    exportRepository.exportAllDataToCsv()
                }
                ExportResult.Success(filename)
            } catch (e: UnsupportedOperationException) {
                ExportResult.Unsupported
            } catch (e: Exception) {
                ExportResult.Failed
            }
            _isExporting.value = false
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appDataRepository.clearAllData()
            }
            _dataCleared.value = true
        }
    }

    fun resetClearedFlag() {
        _dataCleared.value = false
    }
}
