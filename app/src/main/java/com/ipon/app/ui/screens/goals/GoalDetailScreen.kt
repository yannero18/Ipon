package com.ipon.app.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.GoalContribution
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.GoalRingAvatar
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.util.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The GoSave-style hero page for a single goal -- big ring, big number, an
 * "Add funds" action, how much is left to the target, and the full
 * contribution history. Reuses GoalsViewModel (it already loads every
 * goal's progress) rather than a dedicated ViewModel, matching how
 * AddTransactionScreen resolves an ID against an already-loaded list
 * instead of a fresh per-screen query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    viewModelFactory: IponViewModelFactory,
    goalId: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit
) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val progress = uiState.goals.find { it.goal.id == goalId }
    val contributions by viewModel.contributionsForGoal(goalId).collectAsState(initial = emptyList())

    var showAddFunds by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text(progress?.goal?.label ?: "", color = KapeBrown, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KapeBrown)
                    }
                },
                actions = {
                    if (progress != null) {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit goal", tint = KapeBrown)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper)
            )
        }
    ) { padding ->
        if (progress == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) CircularProgressIndicator(color = OceanTeal)
                // If not loading and still null, the goal was just deleted from under us --
                // onBack already covers navigating away, nothing else to render here.
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        GoalRingAvatar(
                            imageUri = progress.goal.imageUri,
                            emoji = progress.goal.emoji,
                            progress = progress.fraction,
                            size = 140.dp,
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = progress.saved.formatPhp(),
                            style = MaterialTheme.typography.displaySmall.merge(TabularNumberStyle),
                            color = KapeBrown,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "of ${progress.goal.target.formatPhp()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddFunds = true },
                            shape = IponShapes.SquircleLg,
                            colors = ButtonDefaults.buttonColors(containerColor = OceanTeal)
                        ) {
                            Text("+ Add funds", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = IponShapes.SquircleLg,
                        colors = CardDefaults.cardColors(containerColor = RicePaperDeep),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Flag, contentDescription = null, tint = KapeBrownSoft)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Target amount", style = MaterialTheme.typography.labelSmall, color = KapeBrownSoft)
                                Text(
                                    text = if (progress.isComplete) "Goal reached! \uD83C\uDF89" else "${progress.remaining.formatPhp()} left",
                                    style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                                    color = KapeBrown,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KapeBrown
                    )
                }

                if (contributions.isEmpty()) {
                    item {
                        Text(
                            text = "No contributions yet. Tap \"Add funds\" to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft
                        )
                    }
                } else {
                    items(contributions, key = { it.id }) { contribution ->
                        ContributionRow(contribution)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showAddFunds && progress != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAddFunds = false },
            sheetState = sheetState,
            containerColor = RicePaper
        ) {
            AddFundsSheetContent(
                goalLabel = progress.goal.label,
                onConfirm = { amount, note ->
                    viewModel.addContribution(progress.goal, amount, note)
                    showAddFunds = false
                },
                onCancel = { showAddFunds = false }
            )
        }
    }
}

@Composable
internal fun ContributionRow(contribution: GoalContribution) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(contribution.contributedAtEpochMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrown
            )
            contribution.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(text = note, style = MaterialTheme.typography.bodySmall, color = KapeBrownSoft)
            }
        }
        Text(
            text = "+${contribution.amount.formatPhp()}",
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
            color = OceanTeal,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun AddFundsSheetContent(
    goalLabel: String,
    onConfirm: (amount: Money, note: String?) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Add to $goalLabel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KapeBrown)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (Php)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text("Cancel", color = KapeBrownSoft)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val parsed = amount.toLongOrNull() ?: 0L
                    if (parsed > 0L) {
                        onConfirm(Money.ofMinorUnits(parsed * 100), note.takeIf { it.isNotBlank() })
                    }
                },
                shape = IponShapes.SquircleSm,
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal)
            ) {
                Text("Add Funds", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
