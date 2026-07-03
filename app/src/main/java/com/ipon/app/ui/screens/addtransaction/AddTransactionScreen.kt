package com.ipon.app.ui.screens.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.FrauncesFamily
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.util.HapticFeedbackManager
import com.ipon.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModelFactory: IponViewModelFactory,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    onDeleted: () -> Unit = onCancel,
    transactionIdToEdit: String? = null
) {
    val viewModel: AddTransactionViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = remember(context) { HapticFeedbackManager(context) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Loads the existing transaction into the form exactly once
    LaunchedEffect(transactionIdToEdit) {
        if (transactionIdToEdit != null) {
            viewModel.loadTransactionForEditing(transactionIdToEdit)
        }
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) {
            onDeleted()
        }
    }

    LaunchedEffect(uiState.amountError) {
        if (uiState.amountError != null) {
            haptics.onValidationError()
        }
    }

    LaunchedEffect(uiState.savedTransactionId) {
        uiState.savedTransactionId?.let { id ->
            val amount = Money.parse(uiState.amountInput) ?: Money.ZERO
            val threshold = Money.parse("1000") ?: Money.ZERO
            haptics.onTransactionSaved(amount, largeAmountThreshold = threshold)
            onSaved(id)
        }
    }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(onClick = onCancel) { 
                        Text("Cancel", color = KapeBrownSoft, style = MaterialTheme.typography.bodyLarge) 
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Delete", color = Terracotta, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Editorial Large Page Title
            Text(
                text = if (uiState.isEditing) "Edit Entry" else "New Entry",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = KapeBrown,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. TYPOGRAPHIC HERO: Massive Centered Currency Input
            BasicTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 54.sp,
                    fontFamily = FrauncesFamily,
                    color = KapeBrown,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₱",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 46.sp,
                                fontFamily = FrauncesFamily,
                                color = KapeBrownSoft
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Box {
                            if (uiState.amountInput.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 54.sp,
                                        fontFamily = FrauncesFamily,
                                        color = KapeBrownSoft.copy(alpha = 0.2f),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            // Centered error message if validation fails
            uiState.amountError?.let { errorText ->
                Text(
                    text = errorText,
                    color = Terracotta,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. MINIMALIST TEXT TABS: Ditch Segmented Sliders Completely
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeToggleOption(
                    label = "Expense",
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChanged(TransactionType.EXPENSE) }
                )
                Spacer(modifier = Modifier.width(36.dp))
                TypeToggleOption(
                    label = "Income",
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChanged(TransactionType.INCOME) }
                )
            }

            // 3. EDITORIAL ALIGNMENT: Merchant / Source input
            Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Text(
                    text = "MERCHANT / SOURCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp, 
                        fontWeight = FontWeight.Bold
                    ),
                    color = KapeBrownSoft
                )
                TextField(
                    value = uiState.merchantRaw ?: "",
                    onValueChange = viewModel::onMerchantChanged,
                    placeholder = { 
                        Text("e.g. STAMARIA-TODA-04", color = KapeBrownSoft.copy(alpha = 0.4f)) 
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = OceanTeal,
                        unfocusedIndicatorColor = KapeBrownSoft.copy(alpha = 0.2f),
                        cursorColor = OceanTeal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 4.dp)
                )
            }

            // 4. ELEVATED CATEGORY SECTION: 3-column Grid of Spacious Soft Cards
            Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp, 
                        fontWeight = FontWeight.Bold
                    ),
                    color = KapeBrownSoft
                )
                
                // Intelligent AI / Memory Category Suggestion Card (Indented and Soft-styled)
                uiState.suggestedCategory?.let { suggestion ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
                            .clip(IponShapes.SquircleSm)
                            .background(OceanTeal.copy(alpha = 0.08f))
                            .border(1.dp, OceanTeal.copy(alpha = 0.15f), IponShapes.SquircleSm)
                            .clickable { viewModel.onCategorySelected(suggestion) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = suggestion.icon(),
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.suggestionIsFromMemory) {
                                "Usually: ${suggestion.displayName} (Tap to select)"
                            } else {
                                "Suggested: ${suggestion.displayName} (Tap to select)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OceanTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val availableCategories: List<TransactionCategory> = when (uiState.type) {
                    TransactionType.EXPENSE -> ExpenseCategory.entries.toList()
                    TransactionType.INCOME -> IncomeCategory.entries.toList()
                }

                // Grid of Spacious Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val columns = 3
                    val chunkedCategories = availableCategories.chunked(columns)
                    chunkedCategories.forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowCategories.forEach { category ->
                                CategoryCard(
                                    category = category,
                                    selected = uiState.selectedCategory == category,
                                    onClick = { viewModel.onCategorySelected(category) }
                                )
                            }
                            // Fill trailing empty columns with spacers to maintain proper grid sizing
                            if (rowCategories.size < columns) {
                                repeat(columns - rowCategories.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 5. EDITORIAL ALIGNMENT: Note Input
            Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
                Text(
                    text = "NOTE (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp, 
                        fontWeight = FontWeight.Bold
                    ),
                    color = KapeBrownSoft
                )
                TextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChanged,
                    placeholder = { 
                        Text("Add any special details...", color = KapeBrownSoft.copy(alpha = 0.4f)) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = OceanTeal,
                        unfocusedIndicatorColor = KapeBrownSoft.copy(alpha = 0.2f),
                        cursorColor = OceanTeal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Primary Action Button
            Button(
                onClick = viewModel::save,
                colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
                shape = IponShapes.SquircleMd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = if (uiState.isEditing) "Save changes" else "Save entry", 
                    color = RicePaper,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("This removes it from your ledger permanently. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    haptics.onDeleteConfirmed()
                    viewModel.delete()
                }) {
                    Text("Delete", color = Terracotta)
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

@Composable
private fun TypeToggleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            color = if (selected) KapeBrown else KapeBrownSoft.copy(alpha = 0.5f),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FrauncesFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Underline representing hand-drawn editorial feel
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(3.dp)
                .clip(IponShapes.SquircleSm)
                .background(if (selected) OceanTeal else Color.Transparent)
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CategoryCard(
    category: TransactionCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .weight(1f)
            .clip(IponShapes.SquircleSm)
            .background(if (selected) OceanTeal else Color.White)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = KapeBrownSoft.copy(alpha = 0.15f),
                shape = IponShapes.SquircleSm
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = if (selected) Color.White else OceanTeal,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.displayName,
            color = if (selected) Color.White else KapeBrown,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
