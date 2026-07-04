package com.ipon.app.ui.screens.envelopes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.OrganicSquircleShape
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.WarmCream
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.util.Money
import com.ipon.app.util.HapticFeedbackManager
import java.util.Calendar

@Composable
fun EnvelopesScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: EnvelopesViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = remember(context) { HapticFeedbackManager(context) }

    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val configuredCategories = uiState.envelopes.map { it.category }.toSet()
    val unconfiguredCategories = ExpenseCategory.entries.filter { it !in configuredCategories && it != ExpenseCategory.OTHER }

    val calendar = remember { Calendar.getInstance() }
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Monthly spending caps",
                    style = MaterialTheme.typography.labelSmall,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState.envelopes.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = com.ipon.app.ui.icons.IponIcons.Alkansya,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = KapeBrown.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No envelopes set yet this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KapeBrownSoft,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        TextButton(onClick = { viewModel.copyForwardFromLastMonth() }) {
                            Text("Copy last month's envelopes", color = OceanTeal, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            items(uiState.envelopes, key = { it.category }) { envelope ->
                EnvelopeCard(
                    envelope = envelope,
                    dayOfMonth = dayOfMonth,
                    daysInMonth = daysInMonth,
                    onClick = { editingCategory = envelope.category },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 80.dp)
                        .clip(OrganicSquircleShape)
                        .clickable { showAddDialog = true }
                        .drawBehind {
                            val strokeWidth = 1.2.dp.toPx()
                            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            drawRoundRect(
                                color = KapeBrown,
                                style = Stroke(width = strokeWidth, pathEffect = dashPathEffect),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                            )
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add envelope",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = OceanTeal
                    )
                }
            }
        }
    }

    editingCategory?.let { category ->
        val existing = uiState.envelopes.find { it.category == category }
        SetCapDialog(
            category = category,
            initialAmount = existing?.cap,
            initialIcon = existing?.customIcon,
            suggestedAmount = uiState.suggestedCapsByCategory[category.displayName],
            haptics = haptics,
            onConfirm = { amount, icon ->
                haptics.onTransactionSaved(amount, Money.ZERO)
                viewModel.setCap(category, amount, icon)
                editingCategory = null
            },
            onRemove = if (existing != null) {
                { 
                    haptics.onDeleteConfirmed()
                    viewModel.removeCap(category)
                    editingCategory = null 
                }
            } else null,
            onDismiss = { editingCategory = null }
        )
    }

    if (showAddDialog) {
        CategoryPickerDialog(
            categories = unconfiguredCategories,
            onSelected = { category ->
                showAddDialog = false
                editingCategory = category
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun EnvelopeCard(
    envelope: EnvelopeProgress,
    dayOfMonth: Int,
    daysInMonth: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = if (envelope.isOverBudget) Terracotta else OceanTeal
    val projection = envelope.projectedAtCurrentPace(dayOfMonth, daysInMonth)
    // Only worth surfacing if the envelope isn't already over (that case
    // already shows "Over by X" below) and there's at least a few days of
    // real spending data to extrapolate from -- a pace projection from day
    // 1 or 2 of the month is too noisy to be a useful nudge.
    val showsPaceWarning = !envelope.isOverBudget && dayOfMonth >= 3 && projection > envelope.cap

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(OrganicSquircleShape)
            .background(WarmCream)
            .border(1.dp, HairlineBorder, OrganicSquircleShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryLabel(
                category = envelope.category,
                customIcon = envelope.customIcon,
                textColor = KapeBrown,
                hasBackground = true
            )
            Text(
                text = "${envelope.spent.formatPhp()} / ${envelope.cap.formatPhp()}",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = if (envelope.isOverBudget) Terracotta else KapeBrownSoft
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
                    .fillMaxWidth(fraction = envelope.fractionUsed.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(barColor)
            )
        }

        if (envelope.isOverBudget) {
            Text(
                text = "Over by ${(-envelope.remaining).formatPhp()}",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = Terracotta,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else if (showsPaceWarning) {
            Text(
                text = "At this pace, projected to reach ${projection.formatPhp()} by month's end",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = Terracotta,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Text(
                text = "${envelope.remaining.formatPhp()} remaining this month",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = KapeBrownSoft,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SetCapDialog(
    category: ExpenseCategory,
    initialAmount: Money?,
    initialIcon: String?,
    suggestedAmount: Money?,
    haptics: HapticFeedbackManager,
    onConfirm: (Money, String?) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var amountInput by remember {
        mutableStateOf(
            initialAmount?.let { (it.minorUnits / 100.0).toString() } ?: ""
        )
    }
    var customIconInput by remember { mutableStateOf(initialIcon ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { CategoryLabel(category = category, customIcon = customIconInput.ifEmpty { null }, text = "${category.displayName} cap", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; error = null },
                    label = { Text("Monthly cap (\u20b1)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = customIconInput,
                    onValueChange = { customIconInput = it.take(2) },
                    label = { Text("Custom Emoji / Icon") },
                    placeholder = { Text("e.g. 🍔, 🏠, ☕") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Quick Emojis:",
                    style = MaterialTheme.typography.labelSmall,
                    color = KapeBrownSoft
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val quickEmojis = listOf("🏠", "🍔", "🚗", "🎒", "💸", "🛒", "🛍️", "💊", "🎬", "📚", "🏛️", "☕", "🎮", "🐷")
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (customIconInput == emoji) JeepneyOrange.copy(alpha = 0.2f) else RicePaperDeep)
                                .clickable { customIconInput = emoji }
                                .border(1.dp, if (customIconInput == emoji) JeepneyOrange else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }
                
                // Only worth suggesting when there's no existing cap to
                // edit -- once a cap is already set, the person is
                // adjusting a deliberate choice, not starting from zero.
                if (initialAmount == null && suggestedAmount != null && !suggestedAmount.isZero) {
                    TextButton(
                        onClick = { amountInput = (suggestedAmount.minorUnits / 100.0).toString(); error = null },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Use ${suggestedAmount.formatPhp()} (your 3-month average)",
                            color = OceanTeal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Money.parse(amountInput)
                if (amount == null || amount.isZero || amount.isNegative) {
                    error = "Enter a valid amount"
                    haptics.onValidationError()
                } else {
                    onConfirm(amount, customIconInput.ifEmpty { null })
                }
            }) {
                Text("Save", color = JeepneyOrange)
            }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = Terracotta)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            }
        }
    )
}

@Composable
private fun CategoryPickerDialog(
    categories: List<ExpenseCategory>,
    onSelected: (ExpenseCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a category") },
        text = {
            Column {
                if (categories.isEmpty()) {
                    Text(
                        text = "Every category already has an envelope.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                } else {
                    categories.forEach { category ->
                        CategoryLabel(
                            category = category,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelected(category) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = KapeBrownSoft) }
        }
    )
}
