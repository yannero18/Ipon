package com.ipon.app.ui.screens.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ipon.app.data.local.RecurrenceFrequency
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.RecurringPatternSuggestion
import com.ipon.app.data.model.RecurringTemplate
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import java.util.Calendar

@Composable
fun RecurringScreen(
    viewModelFactory: IponViewModelFactory,
    onAddTemplateClick: () -> Unit
) {
    val viewModel: RecurringViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Recurring",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Nothing logs itself \u2014 you confirm each one when it's due.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            if (uiState.dueTemplates.isNotEmpty()) {
                item {
                    Text(
                        text = "DUE NOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = Terracotta,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(uiState.dueTemplates, key = { "due-" + it.id }) { template ->
                    DueTemplateCard(
                        template = template,
                        onConfirm = { viewModel.confirm(template) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            if (uiState.detectedPatterns.isNotEmpty()) {
                item {
                    Text(
                        text = "NOTICED A PATTERN",
                        style = MaterialTheme.typography.labelSmall,
                        color = OceanTeal,
                        modifier = Modifier.padding(top = if (uiState.dueTemplates.isNotEmpty()) 18.dp else 0.dp, bottom = 8.dp)
                    )
                }
                items(uiState.detectedPatterns, key = { "pattern-" + it.merchantKey }) { pattern ->
                    DetectedPatternCard(
                        pattern = pattern,
                        onCreateTemplate = { viewModel.createTemplateFromPattern(pattern, dayOfPeriod = currentDayOfMonth()) },
                        onDismiss = { viewModel.dismissPattern(pattern) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            item {
                Text(
                    text = "ALL TEMPLATES",
                    style = MaterialTheme.typography.labelSmall,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(
                        top = if (uiState.dueTemplates.isNotEmpty() || uiState.detectedPatterns.isNotEmpty()) 18.dp else 0.dp,
                        bottom = 8.dp
                    )
                )
            }

            if (uiState.allTemplates.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        text = "No recurring templates yet \u2014 add rent, Meralco, or your sweldo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            items(uiState.allTemplates, key = { it.id }) { template ->
                TemplateRow(
                    template = template,
                    onTogglePaused = { viewModel.togglePaused(template) },
                    onDelete = { viewModel.delete(template) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                OutlinedButton(
                    onClick = onAddTemplateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 80.dp)
                ) {
                    Text("+ New recurring template", color = OceanTeal)
                }
            }
        }
    }
}

@Composable
private fun DetectedPatternCard(
    pattern: RecurringPatternSuggestion,
    onCreateTemplate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .border(1.dp, OceanTeal.copy(alpha = 0.3f), IponShapes.SquircleLg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CategoryLabel(category = pattern.category, text = pattern.merchantRawExample, textColor = KapeBrown)
            Text(
                text = pattern.averageAmount.formatPhp(),
                style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                color = KapeBrown
            )
        }
        Text(
            text = "Logged ${pattern.occurrenceCount} times, roughly " +
                (if (pattern.suggestedFrequency == RecurrenceFrequency.MONTHLY) "every month" else "every week"),
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        Row {
            Button(
                onClick = onCreateTemplate,
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                shape = IponShapes.SquircleSm
            ) {
                Text("Make it recurring", color = RicePaper)
            }
            TextButton(onClick = onDismiss) {
                Text("Not now", color = KapeBrownSoft)
            }
        }
    }
}

private fun currentDayOfMonth(): Int =
    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

@Composable
private fun DueTemplateCard(
    template: RecurringTemplate,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .border(1.dp, Terracotta.copy(alpha = 0.4f), IponShapes.SquircleLg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                CategoryLabel(
                    category = template.category,
                    text = template.label,
                    textColor = KapeBrown
                )
                Text(
                    text = if (template.frequency == RecurrenceFrequency.MONTHLY) "Monthly" else "Weekly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft
                )
            }
            Text(
                text = (if (template.type == TransactionType.EXPENSE) "\u2212" else "+") + template.amount.formatPhp(),
                style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                color = KapeBrown
            )
        }

        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
            shape = IponShapes.SquircleSm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Log it", color = RicePaper)
        }
    }
}

@Composable
private fun TemplateRow(
    template: RecurringTemplate,
    onTogglePaused: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showActions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .border(1.dp, HairlineBorder, IponShapes.SquircleLg)
            .clickable { showActions = !showActions }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                CategoryLabel(
                    category = template.category,
                    text = template.label,
                    textColor = if (template.isPaused) KapeBrownSoft else KapeBrown,
                    tint = if (template.isPaused) KapeBrownSoft else KapeBrown
                )
                Text(
                    text = (if (template.frequency == RecurrenceFrequency.MONTHLY) "Monthly \u00b7 day ${template.dayOfPeriod}" else "Weekly") +
                        if (template.isPaused) " \u00b7 paused" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft
                )
            }
            Text(
                text = template.amount.formatPhp(),
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = KapeBrownSoft
            )
        }

        if (showActions) {
            Row(modifier = Modifier.padding(top = 10.dp)) {
                TextButton(onClick = onTogglePaused) {
                    Text(if (template.isPaused) "Resume" else "Pause", color = OceanTeal)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Terracotta)
                }
            }
        }
    }
}
