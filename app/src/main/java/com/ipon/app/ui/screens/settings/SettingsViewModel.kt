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

    private val _clearDataMessage = MutableStateFlow<String?>(null)
    val clearDataMessage: StateFlow<String?> = _clearDataMessage.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // NEW STATE: To show a loading indicator during the auto-export before wipe
    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

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

    /**
     * The Auto-Export Safety Net!
     * This automatically attempts to write a CSV backup to the Downloads folder
     * BEFORE dropping all tables. If the export fails (e.g. no storage space),
     * it continues with the wipe anyway because the user explicitly requested it.
     */
    fun clearAllData() {
        viewModelScope.launch {
            _isClearing.value = true
            var backupFilename: String? = null
            
            withContext(Dispatchers.IO) {
                try {
                    // Try to save a backup to the Downloads folder first
                    backupFilename = exportRepository.exportAllDataToCsv()
                } catch (e: Exception) {
                    // If the export fails (e.g., old Android version), we proceed 
                    // with the wipe anyway because the user explicitly confirmed it.
                }
                
                // Nuke the database
                appDataRepository.clearAllData()
            }
            
            _isClearing.value = false
            _dataCleared.value = true
            
            // Notify the UI
            if (backupFilename != null) {
                _clearDataMessage.value = "Data cleared. A safety backup was automatically saved to your Downloads folder as \"$backupFilename\"."
            } else {
                _clearDataMessage.value = "Data permanently cleared."
            }
        }
    }

    fun resetClearedFlag() {
        _dataCleared.value = false
    }

    fun resetClearDataMessage() {
        _clearDataMessage.value = null
    }
}