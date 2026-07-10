package com.ipon.app.ui.screens.goals

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

/**
 * The Goals list/hub: a "Total savings" hero, a horizontally scrollable row
 * of ring-avatar pockets (GoTyme's Go Save look), and the full list below.
 * Create and Edit are now real screens reached via [onCreateGoalClick] and
 * tapping into [onGoalClick] rather than AlertDialogs, matching the rest of
 * the app's full-screen forms. "Add funds" stays a quick in-place
 * ModalBottomSheet here -- fast enough for a one-off top-up without leaving
 * the list -- while the same sheet content is reused from
 * [GoalDetailScreen] for the full drill-down view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModelFactory: IponViewModelFactory,
    onCreateGoalClick: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var selectedGoalForAdd by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        containerColor = RicePaper,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGoalClick,
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
                            text = totalSaved.formatPhp(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = KapeBrown,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

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
                                    onClick = { onGoalClick(progress.goal.id) }
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onCreateGoalClick() }
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
                    items(activeGoals, key = { it.goal.id }) { progress ->
                        GoalCard(
                            progress = progress,
                            onAddClick = { selectedGoalForAdd = progress.goal },
                            onClick = { onGoalClick(progress.goal.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    selectedGoalForAdd?.let { goal ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedGoalForAdd = null },
            sheetState = sheetState,
            containerColor = RicePaper
        ) {
            AddFundsSheetContent(
                goalLabel = goal.label,
                onConfirm = { amount, note ->
                    viewModel.addContribution(goal, amount, note)
                    selectedGoalForAdd = null
                },
                onCancel = { selectedGoalForAdd = null }
            )
        }
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onClick() },
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
                    text = "${progress.saved.formatPhp()} / ${progress.goal.target.formatPhp()}",
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
                    Icons.Default.Add,
                    contentDescription = "Done",
                    tint = OceanTeal,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
