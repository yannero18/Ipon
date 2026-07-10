package com.ipon.app.ui.screens.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.util.Money

/**
 * Full-screen "Edit Goal" flow -- same field set, and the same
 * Archive-vs-Delete safety split as the earlier dialog version (Cancel and
 * Save are real, separate actions from Delete; Delete always requires a
 * second explicit confirmation). Only the container changed to a real
 * screen to match CreateGoalScreen and the rest of the app's full-screen
 * forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalScreen(
    viewModelFactory: IponViewModelFactory,
    goalId: String,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val progress = uiState.goals.find { it.goal.id == goalId }
    val goal = progress?.goal

    if (goal == null) {
        Scaffold(containerColor = RicePaper) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) CircularProgressIndicator(color = OceanTeal)
            }
        }
        return
    }

    var label by remember(goal.id) { mutableStateOf(goal.label) }
    var target by remember(goal.id) { mutableStateOf((goal.target.minorUnits / 100).toString()) }
    var deadline by remember(goal.id) { mutableStateOf(goal.deadline ?: "") }
    var selectedEmoji by remember(goal.id) { mutableStateOf(goal.emoji) }
    var selectedImageUri by remember(goal.id) { mutableStateOf(goal.imageUri) }
    var labelError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("Edit Goal", color = KapeBrown, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancel", color = KapeBrownSoft) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            GoalAppearancePicker(
                currentEmoji = selectedEmoji,
                currentUri = selectedImageUri,
                onEmojiSelected = { selectedEmoji = it },
                onUriSelected = { selectedImageUri = it }
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it; labelError = null },
                label = { Text("Label") },
                isError = labelError != null,
                supportingText = labelError?.let { errorText -> { Text(errorText) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
            OutlinedTextField(
                value = target,
                onValueChange = { target = it; amountError = null },
                label = { Text("Target Amount (Php)") },
                isError = amountError != null,
                supportingText = amountError?.let { errorText -> { Text(errorText) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            OutlinedTextField(
                value = deadline,
                onValueChange = { deadline = it },
                label = { Text("Deadline") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )

            Button(
                onClick = {
                    val amount = target.toLongOrNull() ?: 0L
                    val isLabelValid = label.isNotBlank()
                    val isAmountValid = amount > 0L
                    labelError = if (!isLabelValid) "Give it a name" else null
                    amountError = if (!isAmountValid) "Enter a valid target amount" else null
                    if (isLabelValid && isAmountValid) {
                        viewModel.updateGoal(
                            goal.copy(
                                label = label,
                                target = Money.ofMinorUnits(amount * 100),
                                emoji = selectedEmoji,
                                deadline = deadline.takeIf { it.isNotBlank() },
                                imageUri = selectedImageUri
                            )
                        )
                        onDone()
                    }
                },
                shape = IponShapes.SquircleLg,
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    viewModel.archiveGoal(goal)
                    onDone()
                }) {
                    Text("Archive", color = OceanTeal)
                }
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("Delete", color = JeepneyOrange)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this goal?") },
            text = { Text("This removes \"${goal.label}\" and its saved-contribution history permanently. This cannot be undone. If you just want to stop it showing up, Archive is the reversible option instead.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteGoal(goal)
                    onDone()
                }) {
                    Text("Delete", color = JeepneyOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            }
        )
    }
}
