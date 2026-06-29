package com.ipon.app.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.Goal
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.util.Money

private val GOAL_EMOJI_CHOICES = listOf("\ud83c\udfe1", "\ud83c\udfd6\ufe0f", "\ud83c\udfd3", "\ud83d\udc8d", "\ud83c\udf93", "\ud83d\ude91", "\ud83c\udf08", "\ud83d\udcb0")

@Composable
fun GoalsScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var contributingTo by remember { mutableStateOf<Goal?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingGoal by remember { mutableStateOf<Goal?>(null) }

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Goals",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "What you're saving toward",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            if (uiState.goals.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        text = "No goals yet. Start one for a motorcycle, an emergency fund, or anything you're iponing for.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            items(uiState.goals, key = { it.goal.id }) { progress ->
                GoalCard(
                    progress = progress,
                    onAddMoney = { contributingTo = progress.goal },
                    onLongPress = { managingGoal = progress.goal },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 80.dp)
                        .clip(IponShapes.SquircleSm)
                        .background(RicePaperDeep)
                        .clickable { showCreateDialog = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+ New goal", style = MaterialTheme.typography.bodyLarge, color = OceanTeal)
                }
            }
        }
    }

    contributingTo?.let { goal ->
        AddContributionDialog(
            goal = goal,
            onConfirm = { amount ->
                viewModel.addContribution(goal, amount, note = null)
                contributingTo = null
            },
            onDismiss = { contributingTo = null }
        )
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            onConfirm = { label, target, emoji ->
                viewModel.createGoal(label, target, emoji)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    managingGoal?.let { goal ->
        ManageGoalDialog(
            goal = goal,
            onArchive = { viewModel.archiveGoal(goal); managingGoal = null },
            onDelete = { viewModel.deleteGoal(goal); managingGoal = null },
            onDismiss = { managingGoal = null }
        )
    }
}

@Composable
private fun GoalCard(
    progress: GoalProgress,
    onAddMoney: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .clickable(onClick = onLongPress)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${progress.goal.emoji} ${progress.goal.label}",
                style = MaterialTheme.typography.bodyLarge,
                color = KapeBrown
            )
            Text(
                text = "${progress.saved.formatPhp()} / ${progress.goal.target.formatPhp()}",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = KapeBrownSoft
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(RicePaperDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(if (progress.isComplete) OceanTeal else JeepneyOrange)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (progress.isComplete) "Goal reached! \ud83c\udf89" else "${progress.remaining.formatPhp()} to go",
                style = MaterialTheme.typography.bodyMedium,
                color = if (progress.isComplete) OceanTeal else KapeBrownSoft,
                modifier = Modifier.padding(top = 6.dp)
            )
            TextButton(onClick = onAddMoney) {
                Text("+ Add money", color = OceanTeal)
            }
        }
    }
}

@Composable
private fun AddContributionDialog(
    goal: Goal,
    onConfirm: (Money) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${goal.emoji} ${goal.label}") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = null },
                label = { Text("Amount (\u20b1)") },
                isError = error != null,
                supportingText = error?.let { errorText -> { Text(errorText) } }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Money.parse(input)
                if (amount == null || amount.isZero || amount.isNegative) {
                    error = "Enter a valid amount"
                } else {
                    onConfirm(amount)
                }
            }) {
                Text("Add", color = JeepneyOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@Composable
private fun CreateGoalDialog(
    onConfirm: (label: String, target: Money, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(GOAL_EMOJI_CHOICES.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("What are you saving for?") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it; error = null },
                    label = { Text("Target amount (\u20b1)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    GOAL_EMOJI_CHOICES.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(IponShapes.SquircleSm)
                                .background(if (selectedEmoji == emoji) OceanTeal else RicePaperDeep)
                                .clickable { selectedEmoji = emoji }
                                .padding(8.dp)
                        ) {
                            Text(text = emoji)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = Money.parse(targetInput)
                if (target == null || target.isZero || target.isNegative) {
                    error = "Enter a valid target amount"
                } else if (label.isBlank()) {
                    error = "Give it a name"
                } else {
                    onConfirm(label, target, selectedEmoji)
                }
            }) {
                Text("Create", color = JeepneyOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@Composable
private fun ManageGoalDialog(
    goal: Goal,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${goal.emoji} ${goal.label}") },
        text = {
            Text(
                text = "Archiving keeps your contribution history but hides this goal from the list. Deleting removes it and its contributions permanently.",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
        },
        confirmButton = {
            TextButton(onClick = onArchive) { Text("Archive", color = OceanTeal) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = Terracotta) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
            }
        }
    )
}
