package com.ipon.app.ui.screens.goals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.Goal
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.GoalRingAvatar
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.util.Money

@Composable
fun GoalsScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGoalForAdd by remember { mutableStateOf<Goal?>(null) }
    var selectedGoalForEdit by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        containerColor = RicePaper,
        floatingActionButton = {
            // Re-using the FAB for quick creation
            androidx.compose.material3.FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = JeepneyOrange,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Goal", tint = Color.White)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OceanTeal)
            }
        } else {
            val activeGoals = uiState.goals.filter { !it.goal.isArchived }
            val totalSaved = Money.ofMinorUnits(activeGoals.sumOf { it.saved.minorUnits })

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 1. HERO SECTION: "Total Savings" and Pockets Row
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total savings",
                            style = MaterialTheme.typography.labelMedium,
                            color = KapeBrownSoft
                        )
                        Text(
                            text = "Php $totalSaved",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = KapeBrown,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        // Horizontally scrollable row of Goal Ring Avatars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            activeGoals.forEach { progress ->
                                GoalPocketChip(
                                    progress = progress,
                                    onClick = { selectedGoalForAdd = progress.goal }
                                )
                            }
                            
                            // The "Add New" circular button at the end
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { showCreateDialog = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(OceanTeal.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = OceanTeal)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("New", fontSize = 12.sp, color = KapeBrownSoft)
                            }
                        }
                    }
                }

                // 2. LIST SECTION: Active Goals
                if (activeGoals.isEmpty()) {
                    item {
                        Text(
                            text = "No active goals. Tap + to start saving.",
                            color = KapeBrownSoft,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                } else {
                    items(activeGoals) { progress ->
                        GoalCard(
                            progress = progress,
                            onAddClick = { selectedGoalForAdd = progress.goal },
                            onEditClick = { selectedGoalForEdit = progress.goal }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { label, target, emoji, deadline, uri ->
                viewModel.createGoal(label, target, emoji, deadline, uri)
                showCreateDialog = false
            }
        )
    }

    selectedGoalForAdd?.let { goal ->
        AddContributionDialog(
            goal = goal,
            onDismiss = { selectedGoalForAdd = null },
            onConfirm = { amount, note ->
                viewModel.addContribution(goal, amount, note)
                selectedGoalForAdd = null
            }
        )
    }

    selectedGoalForEdit?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = { selectedGoalForEdit = null },
            onConfirm = { updatedGoal ->
                viewModel.updateGoal(updatedGoal)
                selectedGoalForEdit = null
            },
            onArchive = {
                viewModel.archiveGoal(goal)
                selectedGoalForEdit = null
            },
            onDelete = {
                viewModel.deleteGoal(goal)
                selectedGoalForEdit = null
            }
        )
    }
}

@Composable
private fun GoalPocketChip(progress: GoalProgress, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        GoalRingAvatar(
            imageUri = progress.goal.imageUri,
            emoji = progress.goal.emoji,
            progress = progress.fraction,
            size = 56.dp,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = progress.goal.label,
            fontSize = 12.sp,
            color = KapeBrown,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun GoalCard(
    progress: GoalProgress,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = IponShapes.SquircleLg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalRingAvatar(
                imageUri = progress.goal.imageUri,
                emoji = progress.goal.emoji,
                progress = progress.fraction,
                size = 48.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.goal.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = KapeBrown,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Php ${progress.saved} / Php ${progress.goal.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KapeBrownSoft
                )
                progress.goal.deadline?.let {
                    Text(
                        text = "Target: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = OceanTeal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            if (!progress.isComplete) {
                OutlinedButton(
                    onClick = onAddClick,
                    shape = IponShapes.SquircleSm
                ) {
                    Text("+ Add", color = OceanTeal, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(
                    Icons.Default.Add, // Using Add as a placeholder for a checkmark here if desired
                    contentDescription = "Done",
                    tint = OceanTeal,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalAppearancePicker(
    currentEmoji: String,
    currentUri: String?,
    onEmojiSelected: (String) -> Unit,
    onUriSelected: (String?) -> Unit
) {
    val commonEmojis = listOf("🎯", "🏍️", "🏖️", "💻", "🏠", "💍", "✈️", "🎓")
    
    // Launch Android's native secure Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUriSelected(uri.toString())
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Appearance", style = MaterialTheme.typography.labelSmall, color = KapeBrownSoft)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Goal Preview Ring
            GoalRingAvatar(
                imageUri = currentUri,
                emoji = currentEmoji,
                progress = 0.3f, // Mock progress for the preview
                size = 56.dp
            )
            
            // Emoji quick selects
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                commonEmojis.take(4).forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentEmoji == emoji && currentUri == null) OceanTeal.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                onEmojiSelected(emoji)
                                onUriSelected(null) // Clear photo if an emoji is picked
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            // Photo Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OceanTeal.copy(alpha = 0.1f))
                    .clickable { 
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = "Pick Photo", tint = OceanTeal, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, target: Money, emoji: String, deadline: String?, imageUri: String?) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎯") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RicePaper,
        shape = IponShapes.SquircleLg,
        title = { Text("New Goal", color = KapeBrown, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalAppearancePicker(
                    currentEmoji = selectedEmoji,
                    currentUri = selectedImageUri,
                    onEmojiSelected = { selectedEmoji = it },
                    onUriSelected = { selectedImageUri = it }
                )
                
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("What are you saving for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount (Php)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (e.g. Dec 2026) - Optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = target.toLongOrNull() ?: 0L
                    if (label.isNotBlank() && amount > 0L) {
                        onConfirm(
                            label,
                            Money.ofMinorUnits(amount * 100),
                            selectedEmoji,
                            deadline.takeIf { it.isNotBlank() },
                            selectedImageUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal)
            ) {
                Text("Create Goal", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@Composable
fun EditGoalDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (Goal) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(goal.label) }
    var target by remember { mutableStateOf((goal.target.minorUnits / 100).toString()) }
    var deadline by remember { mutableStateOf(goal.deadline ?: "") }
    var selectedEmoji by remember { mutableStateOf(goal.emoji) }
    var selectedImageUri by remember { mutableStateOf(goal.imageUri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RicePaper,
        shape = IponShapes.SquircleLg,
        title = { Text("Edit Goal", color = KapeBrown, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalAppearancePicker(
                    currentEmoji = selectedEmoji,
                    currentUri = selectedImageUri,
                    onEmojiSelected = { selectedEmoji = it },
                    onUriSelected = { selectedImageUri = it }
                )
                
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount (Php)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = target.toLongOrNull() ?: 0L
                    if (label.isNotBlank() && amount > 0L) {
                        onConfirm(
                            goal.copy(
                                label = label,
                                target = Money.ofMinorUnits(amount * 100),
                                emoji = selectedEmoji,
                                deadline = deadline.takeIf { it.isNotBlank() },
                                imageUri = selectedImageUri
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("Delete", color = JeepneyOrange) }
        }
    )
}

@Composable
fun AddContributionDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (amount: Money, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RicePaper,
        shape = IponShapes.SquircleLg,
        title = { Text("Add to ${goal.label}", color = KapeBrown, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (Php)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.toLongOrNull() ?: 0L
                    if (parsed > 0L) {
                        onConfirm(Money.ofMinorUnits(parsed * 100), note.takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal)
            ) {
                Text("Add Funds", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}