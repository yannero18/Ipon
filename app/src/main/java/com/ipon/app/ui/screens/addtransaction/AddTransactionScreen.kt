package com.ipon.app.ui.screens.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.Category
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.util.HapticCurrencyFeedback
import com.ipon.app.util.Money

@Composable
fun AddTransactionScreen(
    viewModelFactory: IponViewModelFactory,
    onSaved: (transactionId: String) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddTransactionViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = remember(context) { HapticCurrencyFeedback(context) }

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
                title = { Text("New entry", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancel", color = KapeBrownSoft) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14000000))
                    .padding(4.dp)
            ) {
                TypeToggleOption(
                    label = "Expense",
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChanged(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f)
                )
                TypeToggleOption(
                    label = "Income",
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChanged(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Amount (\u20b1)") },
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { errorText -> { Text(errorText) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )

            OutlinedTextField(
                value = uiState.merchantRaw,
                onValueChange = viewModel::onMerchantChanged,
                label = { Text("Merchant / source") },
                placeholder = { Text("e.g. STAMARIA-TODA-04") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            uiState.suggestedCategory?.let { suggestion ->
                Text(
                    text = "Suggested: ${suggestion.emoji} ${suggestion.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTeal,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
            }

            Text(
                text = "Category",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.entries.toList()) { category ->
                    CategoryChip(
                        category = category,
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("Note (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            )

            Button(
                onClick = viewModel::save,
                colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
                shape = IponShapes.SquircleSm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Save entry", color = RicePaper)
            }
        }
    }
}

@Composable
private fun TypeToggleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OceanTeal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) RicePaper else KapeBrownSoft,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(IponShapes.SquircleSm)
            .background(if (selected) OceanTeal else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = category.emoji, modifier = Modifier.padding(end = 6.dp))
        Text(
            text = category.displayName,
            color = if (selected) RicePaper else KapeBrown,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
