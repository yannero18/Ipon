package com.ipon.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.BuildConfig
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.Terracotta

@Composable
fun SettingsScreen(
    viewModelFactory: IponViewModelFactory,
    onLearnedCategoriesClick: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val dataCleared by viewModel.dataCleared.collectAsState()
    val clearDataMessage by viewModel.clearDataMessage.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showFinalConfirmDialog by remember { mutableStateOf(false) }

    // Automatically close the confirmation dialogs when the data wipe finishes
    LaunchedEffect(dataCleared) {
        if (dataCleared) {
            showConfirmDialog = false
            showFinalConfirmDialog = false
            viewModel.resetClearedFlag()
        }
    }

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = KapeBrown,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                SettingsSection(title = "About") {
                    InfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
                    InfoRow(label = "Build", value = BuildConfig.VERSION_CODE.toString())
                }

                SettingsSection(title = "Your privacy") {
                    Text(
                        text = "Ipon has no login, no account, and no cloud sync. " +
                            "Everything you log lives only in this app's local database " +
                            "on this device. This app does not request internet access " +
                            "at all -- it is not just unused, it is not in the app's " +
                            "permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                    Text(
                        text = "Because of that, there's no automatic cloud backup. " +
                            "Use \"Export your data\" below any time you want a copy " +
                            "you control, saved to your phone's Downloads folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                SettingsSection(title = "Personalization") {
                    Text(
                        text = "Ipon remembers which category you pick for each merchant, " +
                            "so it can suggest it again next time -- built entirely from " +
                            "your own corrections, never shared anywhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedButton(
                        onClick = onLearnedCategoriesClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Your learned categories", color = OceanTeal, fontWeight = FontWeight.Bold)
                    }
                }

                SettingsSection(title = "Export your data") {
                    Text(
                        text = "Saves everything -- transactions, envelopes, recurring " +
                            "templates, goals, and reflections -- as a CSV file in your " +
                            "Downloads folder, openable in any spreadsheet app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.exportData() },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                        shape = IponShapes.SquircleSm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExporting && !showFinalConfirmDialog) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = RicePaper,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Export to CSV", color = RicePaper, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                SettingsSection(title = "Data") {
                    Text(
                        text = "Permanently erases every transaction, envelope, recurring " +
                            "template, goal, contribution, and reflection on this device. " +
                            "Ipon will attempt to auto-save a safety backup to your Downloads folder before wiping.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedButton(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear all data", color = Terracotta, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    exportResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportResult() },
            title = {
                Text(
                    text = when (result) {
                        is ExportResult.Success -> "Exported"
                        ExportResult.Unsupported -> "Not available"
                        ExportResult.Failed -> "Export failed"
                    },
                    color = KapeBrown,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = when (result) {
                        is ExportResult.Success -> "Saved as \"${result.filename}\" in your Downloads folder."
                        ExportResult.Unsupported -> "CSV export needs Android 10 or newer. This device's Android version doesn't support it."
                        ExportResult.Failed -> "Something went wrong while writing the file."
                    },
                    color = KapeBrownSoft
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearExportResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Clear all data?", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This deletes your entire ledger, all envelopes, recurring " +
                        "templates, goals, and reflections.\n\n" +
                        "If you haven't exported a copy, this is permanent. This cannot be undone.",
                    color = KapeBrownSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    showFinalConfirmDialog = true
                }) {
                    Text("Continue", color = Terracotta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }

    if (showFinalConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearing) showFinalConfirmDialog = false },
            title = { Text("Are you absolutely sure?", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This is your last chance to back out. Everything will be permanently deleted.", color = KapeBrownSoft)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "🛡️ As a safety net, an automatic CSV backup will be saved to your Downloads folder before the data is erased.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAllData() },
                    enabled = !isClearing,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    shape = IponShapes.SquircleSm
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete everything", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinalConfirmDialog = false },
                    enabled = !isClearing
                ) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }

    clearDataMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.resetClearDataMessage() },
            title = { Text("Data Cleared", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = KapeBrownSoft) },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetClearDataMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = KapeBrownSoft,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .padding(16.dp)
    ) {
        content()
    }
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = KapeBrown, fontWeight = FontWeight.Medium)
    }
}