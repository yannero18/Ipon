package com.ipon.app.ui.screens.debts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.ipon.app.data.model.Debt
import com.ipon.app.data.model.DebtProgress
import com.ipon.app.data.model.PayoffMethod
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
import com.ipon.app.util.HapticFeedbackManager
import com.ipon.app.util.Money

@Composable
fun DebtsScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: DebtsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = remember(context) { HapticFeedbackManager(context) }

    var payingDebt by remember { mutableStateOf<Debt?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingDebt by remember { mutableStateOf<Debt?>(null) }

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Debts",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "What you're paying off, in the order that helps most",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14000000))
                        .padding(4.dp)
                ) {
                    MethodOption(
                        label = "Snowball",
                        selected = uiState.method == PayoffMethod.SNOWBALL,
                        onClick = { viewModel.setMethod(PayoffMethod.SNOWBALL) },
                        modifier = Modifier.weight(1f)
                    )
                    MethodOption(
                        label = "Avalanche",
                        selected = uiState.method == PayoffMethod.AVALANCHE,
                        onClick = { viewModel.setMethod(PayoffMethod.AVALANCHE) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = if (uiState.method == PayoffMethod.SNOWBALL) {
                        "Smallest balance first -- quick wins to build momentum."
                    } else {
                        "Highest interest rate first -- pays the least interest overall."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
            }

            if (uiState.orderedDebts.isEmpty() && uiState.paidOffDebts.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = com.ipon.app.ui.icons.IponIcons.Alkansya,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = KapeBrown.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No debts tracked. If you're paying off a card, a 5-6 loan, " +
                                    "or any utang, add it here to see a clear payoff order.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KapeBrownSoft,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            itemsIndexed(uiState.orderedDebts) { index, progress ->
                DebtCard(
                    progress = progress,
                    payoffOrder = index + 1,
                    onAddPayment = { payingDebt = progress.debt },
                    onLongPress = { managingDebt = progress.debt },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (uiState.paidOffDebts.isNotEmpty()) {
                item {
                    Text(
                        text = "PAID OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = OceanTeal,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                items(uiState.paidOffDebts, key = { it.debt.id }) { progress ->
                    Text(
                        text = "\u2713 ${progress.debt.label}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OceanTeal,
                        modifier = Modifier
                            .clickable { managingDebt = progress.debt }
                            .padding(vertical = 6.dp)
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 80.dp)
                        .clip(IponShapes.SquircleSm)
                        .background(RicePaperDeep)
                        .clickable { showCreateDialog = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+ Add debt", style = MaterialTheme.typography.bodyLarge, color = OceanTeal)
                }
            }
        }
    }

    payingDebt?.let { debt ->
        AddPaymentDialog(
            debt = debt,
            haptics = haptics,
            onConfirm = { amount ->
                haptics.onTransactionSaved(amount, Money.ZERO)
                viewModel.recordPayment(debt, amount)
                payingDebt = null
            },
            onDismiss = { payingDebt = null }
        )
    }

    if (showCreateDialog) {
        CreateDebtDialog(
            haptics = haptics,
            onConfirm = { label, balance, rate ->
                haptics.onTransactionSaved(balance, Money.ZERO)
                viewModel.createDebt(label, balance, rate)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    managingDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { managingDebt = null },
            title = { Text(debt.label) },
            text = {
                Text(
                    "Archiving keeps your payment history but hides this debt from the list. " +
                            "Deleting removes it and its payments permanently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.archiveDebt(debt); managingDebt = null }) {
                    Text("Archive", color = OceanTeal)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { 
                        haptics.onDeleteConfirmed()
                        viewModel.deleteDebt(debt)
                        managingDebt = null 
                    }) {
                        Text("Delete", color = Terracotta)
                    }
                    TextButton(onClick = { managingDebt = null }) {
                        Text("Cancel", color = KapeBrownSoft)
                    }
                }
            }
        )
    }
}

@Composable
private fun MethodOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OceanTeal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (selected) RicePaper else KapeBrownSoft, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DebtCard(
    progress: DebtProgress,
    payoffOrder: Int,
    onAddPayment: () -> Unit,
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JeepneyOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("#$payoffOrder", style = MaterialTheme.typography.bodyMedium, color = JeepneyOrange)
                }
                Text(
                    text = progress.debt.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KapeBrown,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            progress.debt.interestRatePercent?.let { rate ->
                Text(text = "$rate% APR", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
            }
        }

        Text(
            text = "${progress.remaining.formatPhp()} left of ${progress.debt.originalBalance.formatPhp()}",
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
            color = KapeBrownSoft,
            modifier = Modifier.padding(top = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(RicePaperDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.fractionPaid.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(OceanTeal)
            )
        }

        TextButton(onClick = onAddPayment, modifier = Modifier.padding(top = 8.dp)) {
            Text("+ Record payment", color = OceanTeal)
        }
    }
}

@Composable
private fun AddPaymentDialog(debt: Debt, haptics: HapticFeedbackManager, onConfirm: (Money) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment toward ${debt.label}") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = null },
                label = { Text("Amount (₱)") },
                isError = error != null,
                supportingText = error?.let { errorText -> { Text(errorText) } }
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
                Text("Record", color = JeepneyOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@Composable
private fun CreateDebtDialog(
    haptics: HapticFeedbackManager,
    onConfirm: (label: String, balance: Money, ratePercent: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New debt") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("What is it?") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it; error = null },
                    label = { Text("Current balance (₱)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Interest rate %, optional") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val balance = Money.parse(balanceInput)
                if (balance == null || balance.isZero || balance.isNegative) {
                    error = "Enter a valid balance"
                    haptics.onValidationError()
                } else if (label.isBlank()) {
                    error = "Give it a name"
                    haptics.onValidationError()
                } else {
                    val rate = rateInput.toDoubleOrNull()
                    onConfirm(label, balance, rate)
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
