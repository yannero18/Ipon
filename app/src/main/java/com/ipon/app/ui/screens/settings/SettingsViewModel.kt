package com.ipon.app.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipon.app.data.backup.IponBackup
import com.ipon.app.data.repository.AppDataRepository
import com.ipon.app.data.repository.BackupRepository
import com.ipon.app.data.repository.BackupSummary
import com.ipon.app.data.repository.BackupTooNewException
import com.ipon.app.data.repository.ExportRepository
import com.ipon.app.data.repository.InvalidBackupFileException
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

sealed interface BackupExportResult {
    data class Success(val filename: String) : BackupExportResult
    data object Unsupported : BackupExportResult
    data object Failed : BackupExportResult
}

/** A backup file the person picked, read and validated, waiting on their explicit "yes, replace everything" confirmation. */
sealed interface PendingRestore {
    data class ReadyToConfirm(val uri: Uri, val backup: IponBackup) : PendingRestore
    data class TooNew(val fileSchemaVersion: Int) : PendingRestore
    data object InvalidFile : PendingRestore
    data object UnreadableFile : PendingRestore
}

sealed interface RestoreResult {
    data class Success(val summary: BackupSummary) : RestoreResult
    data object Failed : RestoreResult
}

class SettingsViewModel(
    private val appDataRepository: AppDataRepository,
    private val exportRepository: ExportRepository,
    private val backupRepository: BackupRepository
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

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupExportResult = MutableStateFlow<BackupExportResult?>(null)
    val backupExportResult: StateFlow<BackupExportResult?> = _backupExportResult.asStateFlow()

    private val _isReadingBackupFile = MutableStateFlow(false)
    val isReadingBackupFile: StateFlow<Boolean> = _isReadingBackupFile.asStateFlow()

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _restoreResult = MutableStateFlow<RestoreResult?>(null)
    val restoreResult: StateFlow<RestoreResult?> = _restoreResult.asStateFlow()

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

    fun backupData() {
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupExportResult.value = try {
                val filename = withContext(Dispatchers.IO) { backupRepository.exportBackup() }
                BackupExportResult.Success(filename)
            } catch (e: UnsupportedOperationException) {
                BackupExportResult.Unsupported
            } catch (e: Exception) {
                BackupExportResult.Failed
            }
            _isBackingUp.value = false
        }
    }

    fun clearBackupExportResult() {
        _backupExportResult.value = null
    }

    /**
     * Step 1 of restore: read and validate the picked file, but don't touch
     * the database yet. The UI shows [PendingRestore.ReadyToConfirm]'s row
     * counts as a real "this replaces your current data with this" warning
     * before [confirmRestore] is ever called.
     */
    fun onRestoreFileSelected(uri: Uri) {
        viewModelScope.launch {
            _isReadingBackupFile.value = true
            _pendingRestore.value = try {
                val backup = withContext(Dispatchers.IO) { backupRepository.peekBackup(uri) }
                PendingRestore.ReadyToConfirm(uri, backup)
            } catch (e: BackupTooNewException) {
                PendingRestore.TooNew(e.fileSchemaVersion)
            } catch (e: InvalidBackupFileException) {
                PendingRestore.InvalidFile
            } catch (e: Exception) {
                PendingRestore.UnreadableFile
            }
            _isReadingBackupFile.value = false
        }
    }

    fun cancelPendingRestore() {
        _pendingRestore.value = null
    }

    /** Step 2: the person has seen the row counts and explicitly confirmed. This is the destructive part. */
    fun confirmRestore() {
        val pending = _pendingRestore.value as? PendingRestore.ReadyToConfirm ?: return
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreResult.value = try {
                val summary = withContext(Dispatchers.IO) { backupRepository.importBackup(pending.backup) }
                RestoreResult.Success(summary)
            } catch (e: Exception) {
                RestoreResult.Failed
            }
            _isRestoring.value = false
            _pendingRestore.value = null
        }
    }

    fun clearRestoreResult() {
        _restoreResult.value = null
    }
}