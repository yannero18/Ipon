package com.ipon.app.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.Goal
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.*
import com.ipon.app.util.Money

private val GOAL_EMOJI_CHOICES = listOf("\ud83c\udfe1", "\ud83c\udfd6\ufe0f", "\ud83c\udfd3", "\ud83d\udc8d", "\ud83c\udf93", "\ud83d\ude91", "\ud83c\udf08", "\ud83d\udcb0")

@Composable
fun GoalsScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = remember(context) { com.ipon.app.util.HapticFeedbackManager(context) }

    var contributingTo by remember { mutableStateOf<Goal?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingGoal by remember { mutableStateOf<Goal?>(null) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 1. Screen Title
            item {
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(
                        text = "SAVINGS FLOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = KapeBrownSoft,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Ipon Goals",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrown
                    )
                }
            }

            // 2. Beautiful Savings Overview Hero Card
            if (uiState.goals.isNotEmpty()) {
                item {
                    val activeGoalsCount = uiState.goals.size
                    val totalSaved = Money.ofMinorUnits(uiState.goals.sumOf { it.saved.minorUnits })
                    val totalTarget = Money.ofMinorUnits(uiState.goals.sumOf { it.goal.target.minorUnits })
                    val averageFraction = if (totalTarget.isZero) 0f else totalSaved.minorUnits.toFloat() / totalTarget.minorUnits.toFloat()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = IponShapes.SquircleLg,
                        colors = CardDefaults.cardColors(containerColor = OceanTeal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "TOTAL SAVED BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = RicePaper.copy(alpha = 0.75f)
                            )
                            Text(
                                text = totalSaved.formatPhp(),
                                style = MaterialTheme.typography.headlineLarge.merge(TabularNumberStyle),
                                color = RicePaper,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "Across $activeGoalsCount active savings goal${if (activeGoalsCount == 1) "" else "s"} (Target: ${totalTarget.formatPhp()})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RicePaper.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Overall Progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RicePaper
                                )
                                Text(
                                    text = "${(averageFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                                    color = RicePaper,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(RicePaper.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = averageFraction.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(JeepneyOrange)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Goals List Title Header
            if (uiState.goals.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Goals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrown,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // 4. Empty State
            if (uiState.goals.isEmpty() && !uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, HairlineBorder, IponShapes.SquircleLg),
                        shape = IponShapes.SquircleLg,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = com.ipon.app.ui.icons.IponIcons.Alkansya,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = KapeBrown.copy(alpha = 0.12f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Start an Ipon Goal",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = KapeBrown,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Set targets for a motorcycle, an emergency fund, travels, or anything you're saving for. Track progress step-by-step.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KapeBrownSoft,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // 5. Goals List
            items(uiState.goals, key = { it.goal.id }) { progress ->
                GoalCard(
                    progress = progress,
                    onAddMoney = { contributingTo = progress.goal },
                    onEditGoal = { editingGoal = progress.goal },
                    onManageGoal = { managingGoal = progress.goal },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 6. Action Button
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleLg,
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(text = "+ Set a New Savings Goal", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }

    // --- DIALOGS ---

    contributingTo?.let { goal ->
        AddContributionDialog(
            goal = goal,
            haptics = haptics,
            onConfirm = { amount ->
                viewModel.addContribution(goal, amount, note = null)
                haptics.onTransactionSaved(amount, Money.ZERO)
                contributingTo = null
            },
            onDismiss = { contributingTo = null }
        )
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            haptics = haptics,
            onConfirm = { label, target, emoji, deadline ->
                viewModel.createGoal(label, target, emoji, deadline)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingGoal?.let { goal ->
        EditGoalDialog(
            goal = goal,
            haptics = haptics,
            onConfirm = { label, target, emoji, deadline ->
                viewModel.updateGoal(goal.copy(label = label, target = target, emoji = emoji, deadline = deadline))
                editingGoal = null
            },
            onDismiss = { editingGoal = null }
        )
    }

    managingGoal?.let { goal ->
        ManageGoalDialog(
            goal = goal,
            onArchive = { viewModel.archiveGoal(goal); managingGoal = null },
            onDelete = { 
                haptics.onDeleteConfirmed()
                viewModel.deleteGoal(goal)
                managingGoal = null 
            },
            onDismiss = { managingGoal = null }
        )
    }
}

@Composable
private fun GoalCard(
    progress: GoalProgress,
    onAddMoney: () -> Unit,
    onEditGoal: () -> Unit,
    onManageGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, IponShapes.SquircleLg),
        shape = IponShapes.SquircleLg,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Emoji badge, label, and percentage tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(IponShapes.SquircleSm)
                            .background(OceanTeal.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = progress.goal.label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (progress.isComplete) "Goal reached!" else "${progress.remaining.formatPhp()} to go",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (progress.isComplete) OceanTeal else KapeBrownSoft
                            )
                            if (!progress.isComplete && !progress.goal.deadline.isNullOrBlank()) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KapeBrownSoft
                                )
                                Text(
                                    text = "📅 ${progress.goal.deadline}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Terracotta,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Percentage Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (progress.isComplete) Color(0xFFE0F2F1) else Color(0xFFFFF3E0))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${(progress.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (progress.isComplete) OceanTeal else JeepneyOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(RicePaperDeep)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.fraction.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(if (progress.isComplete) OceanTeal else JeepneyOrange)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Numerical Progress Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${progress.saved.formatPhp()} of ${progress.goal.target.formatPhp()}",
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = KapeBrownSoft
                )
                
                // Action Buttons Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit Target icon button
                    IconButton(
                        onClick = onEditGoal,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Target Amount",
                            tint = KapeBrownSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Manage / Settings icon button
                    IconButton(
                        onClick = onManageGoal,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Goal Actions",
                            tint = KapeBrownSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    TextButton(
                        onClick = onAddMoney,
                        colors = ButtonDefaults.textButtonColors(contentColor = OceanTeal),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+ Add Money", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddContributionDialog(
    goal: Goal,
    haptics: com.ipon.app.util.HapticFeedbackManager,
    onConfirm: (Money) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${goal.label}", color = KapeBrown, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = null },
                label = { Text("Amount (₱)") },
                isError = error != null,
                supportingText = error?.let { errorText -> { Text(errorText) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Money.parse(input)
                if (amount == null || amount.isZero || amount.isNegative) {
                    error = "Enter a valid amount"
                    haptics.onValidationError()
                } else {
                    onConfirm(amount)
                }
            }) {
                Text("Add", color = OceanTeal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@Composable
private fun CreateGoalDialog(
    haptics: com.ipon.app.util.HapticFeedbackManager,
    onConfirm: (label: String, target: Money, emoji: String, deadline: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }
    var deadlineInput by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(GOAL_EMOJI_CHOICES.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a New Savings Goal", color = KapeBrown, fontWeight = FontWeight.Bold) },
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
                    label = { Text("Target amount (₱)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = deadlineInput,
                    onValueChange = { deadlineInput = it },
                    label = { Text("Target Deadline (optional, e.g. Dec 2026)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    placeholder = { Text("e.g. Dec 2026, or 2026-12-31") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = Money.parse(targetInput)
                if (target == null || target.isZero || target.isNegative) {
                    error = "Enter a valid target amount"
                    haptics.onValidationError()
                } else if (label.isBlank()) {
                    error = "Give it a name"
                    haptics.onValidationError()
                } else {
                    onConfirm(label, target, "🎯", deadlineInput.takeIf { it.isNotBlank() })
                }
            }) {
                Text("Create", color = OceanTeal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@Composable
private fun EditGoalDialog(
    goal: Goal,
    haptics: com.ipon.app.util.HapticFeedbackManager,
    onConfirm: (label: String, target: Money, emoji: String, deadline: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(goal.label) }
    var targetInput by remember { mutableStateOf((goal.target.minorUnits / 100.0).toString()) }
    var deadlineInput by remember { mutableStateOf(goal.deadline ?: "") }
    var selectedEmoji by remember { mutableStateOf(goal.emoji) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Goal Details", color = KapeBrown, fontWeight = FontWeight.Bold) },
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
                    label = { Text("Target amount (₱)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = deadlineInput,
                    onValueChange = { deadlineInput = it },
                    label = { Text("Target Deadline (optional, e.g. Dec 2026)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    placeholder = { Text("e.g. Dec 2026, or 2026-12-31") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = Money.parse(targetInput)
                if (target == null || target.isZero || target.isNegative) {
                    error = "Enter a valid target amount"
                    haptics.onValidationError()
                } else if (label.isBlank()) {
                    error = "Give it a name"
                    haptics.onValidationError()
                } else {
                    onConfirm(label, target, "🎯", deadlineInput.takeIf { it.isNotBlank() })
                }
            }) {
                Text("Save Changes", color = OceanTeal, fontWeight = FontWeight.Bold)
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
        title = { Text(goal.label, color = KapeBrown, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Archiving keeps your contribution history but hides this goal from the active list. Deleting removes the goal and all its logged contributions permanently.",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
        },
        confirmButton = {
            TextButton(onClick = onArchive) { Text("Archive Goal", color = OceanTeal, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) { Text("Delete Permanently", color = Terracotta, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
            }
        }
    )
}
