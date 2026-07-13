package com.ipon.app.ui.screens.debts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.Debt
import com.ipon.app.data.model.DebtProgress
import com.ipon.app.data.model.PayoffMethod
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.OrganicSquircleShape
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.ui.theme.WarmCream
import com.ipon.app.util.HapticFeedbackManager
import com.ipon.app.util.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Debt due dates are stored as plain ISO "yyyy-MM-dd" strings -- unlike
 * Goal.deadline, which is decorative free text, a Debt's due date needs to
 * be genuinely comparable (for the overdue highlight below and for showing
 * up on the Calendar tab), so it's picked from a real date picker rather
 * than typed.
 */
private fun parseIsoDateOrNull(iso: String?): Calendar? {
    if (iso.isNullOrBlank()) return null
    val parts = iso.split("-")
    if (parts.size != 3) return null
    return try {
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    } catch (e: NumberFormatException) {
        null
    }
}

private fun formatIsoDateForDisplay(iso: String): String {
    val cal = parseIsoDateOrNull(iso) ?: return iso
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(cal.time)
}

private fun isDebtOverdue(debt: Debt, isPaidOff: Boolean): Boolean {
    if (isPaidOff) return false
    val due = parseIsoDateOrNull(debt.dueDate) ?: return false
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return due.before(today)
}

/** DatePicker gives UTC-midnight millis for the picked day; converting via a UTC calendar avoids an off-by-one-day shift from local timezone reinterpretation. */
private fun utcMillisToIsoDate(utcMillis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = utcMillis
    return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: DebtsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = remember(context) { HapticFeedbackManager(context) }

    var payingDebt by remember { mutableStateOf<Debt?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingDebt by remember { mutableStateOf<Debt?>(null) }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
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
                    style = MaterialTheme.typography.labelSmall,
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
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        color = OceanTeal,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(uiState.paidOffDebts, key = { it.debt.id }) { progress ->
                    DebtCard(
                        progress = progress,
                        payoffOrder = 1,
                        onAddPayment = {},
                        onLongPress = { managingDebt = progress.debt },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 80.dp)
                        .clip(OrganicSquircleShape)
                        .clickable { showCreateDialog = true }
                        .drawBehind {
                            val strokeWidth = 1.2.dp.toPx()
                            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            drawRoundRect(
                                color = KapeBrownSoft.copy(alpha = 0.5f),
                                style = Stroke(width = strokeWidth, pathEffect = dashPathEffect),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                            )
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add debt",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = OceanTeal
                    )
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
            onConfirm = { label, balance, rate, fee, dueDate ->
                haptics.onTransactionSaved(balance, Money.ZERO)
                viewModel.createDebt(label, balance, rate, fee, dueDate)
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
                Row {
                    TextButton(onClick = { editingDebt = debt; managingDebt = null }) {
                        Text("Edit", color = OceanTeal)
                    }
                    TextButton(onClick = { viewModel.archiveDebt(debt); managingDebt = null }) {
                        Text("Archive", color = OceanTeal)
                    }
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

    editingDebt?.let { debt ->
        EditDebtDialog(
            debt = debt,
            haptics = haptics,
            onConfirm = { updated ->
                viewModel.updateDebt(updated)
                editingDebt = null
            },
            onDismiss = { editingDebt = null }
        )
    }
}

@Composable
private fun MethodOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, 
            color = if (selected) KapeBrown else KapeBrownSoft, 
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
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
    val isPaid = progress.isPaidOff

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(OrganicSquircleShape)
            .background(WarmCream)
            .border(1.dp, HairlineBorder, OrganicSquircleShape)
            .clickable(onClick = onLongPress)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.debt.label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrown
                    )
                    
                    if (isPaid) {
                        Text(
                            text = "Fully paid off",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        val rateText = if (progress.debt.interestRatePercent != null) {
                            "${progress.debt.interestRatePercent}% interest"
                        } else {
                            "0% interest"
                        }
                        Text(
                            text = "${progress.debt.originalBalance.formatPhp()} total · $rateText",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (!progress.debt.fee.isZero) {
                            Text(
                                text = "${progress.debt.fee.formatPhp()} fee · received ${progress.debt.netProceedsReceived.formatPhp()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KapeBrownSoft,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        progress.debt.dueDate?.let { iso ->
                            val overdue = isDebtOverdue(progress.debt, isPaid)
                            Text(
                                text = if (overdue) "Overdue since ${formatIsoDateForDisplay(iso)}" else "Due ${formatIsoDateForDisplay(iso)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal),
                                color = if (overdue) Terracotta else KapeBrownSoft,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isPaid) Color(0xFFE5EFE5) else OceanTeal.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "PRIORITY $payoffOrder",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isPaid) Color(0xFF1D5D6B) else OceanTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isPaid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total payoff",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                    Text(
                        text = progress.debt.originalBalance.formatPhp(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).merge(TabularNumberStyle),
                        color = KapeBrown
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paid Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                    Text(
                        text = "${progress.paid.formatPhp()} / ${progress.debt.originalBalance.formatPhp()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).merge(TabularNumberStyle),
                        color = KapeBrown
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(RicePaperDeep)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.fractionPaid.coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(OceanTeal)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${progress.remaining.formatPhp()} left",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        color = KapeBrownSoft
                    )
                    Text(
                        text = "+ Add payment",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = OceanTeal,
                        modifier = Modifier.clickable(onClick = onAddPayment)
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateField(
    isoDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Due date, optional",
            style = MaterialTheme.typography.labelSmall,
            color = KapeBrownSoft
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, HairlineBorder, RoundedCornerShape(4.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = if (isoDate.isBlank()) "Tap to set a date" else formatIsoDateForDisplay(isoDate),
                color = if (isoDate.isBlank()) KapeBrownSoft.copy(alpha = 0.7f) else KapeBrown
            )
        }
    }

    if (showPicker) {
        val initialMillis = parseIsoDateOrNull(isoDate)?.timeInMillis
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(utcMillisToIsoDate(it)) }
                    showPicker = false
                }) {
                    Text("OK", color = OceanTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDebtDialog(
    debt: Debt,
    haptics: HapticFeedbackManager,
    onConfirm: (Debt) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(debt.label) }
    var balanceInput by remember { mutableStateOf((debt.originalBalance.minorUnits / 100).toString()) }
    var rateInput by remember { mutableStateOf(debt.interestRatePercent?.toString() ?: "") }
    var feeInput by remember { mutableStateOf(if (debt.fee.isZero) "" else (debt.fee.minorUnits / 100).toString()) }
    var dueDateIso by remember { mutableStateOf(debt.dueDate ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val balance = Money.parse(balanceInput)
    val fee = Money.parse(feeInput)
    val netReceived = if (balance != null && fee != null) balance - fee else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit debt") },
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
                    label = { Text("Amount you owe (₱)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = feeInput,
                    onValueChange = { feeInput = it },
                    label = { Text("Fee deducted upfront, optional") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                if (netReceived != null && fee != null && !fee.isZero) {
                    Text(
                        text = "You actually received: ${netReceived.formatPhp()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = OceanTeal,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Interest rate %, optional") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                DueDateField(
                    isoDate = dueDateIso,
                    onDateSelected = { dueDateIso = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (balance == null || balance.isZero || balance.isNegative) {
                    error = "Enter a valid balance"
                    haptics.onValidationError()
                } else if (label.isBlank()) {
                    error = "Give it a name"
                    haptics.onValidationError()
                } else if (fee != null && (fee.isNegative || fee.minorUnits > balance.minorUnits)) {
                    error = "Fee can't be more than the amount you owe"
                    haptics.onValidationError()
                } else {
                    onConfirm(
                        debt.copy(
                            label = label,
                            originalBalance = balance,
                            interestRatePercent = rateInput.toDoubleOrNull(),
                            fee = fee ?: Money.ZERO,
                            dueDate = dueDateIso.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }) {
                Text("Save", color = JeepneyOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = KapeBrownSoft) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateDebtDialog(
    haptics: HapticFeedbackManager,
    onConfirm: (label: String, balance: Money, ratePercent: Double?, fee: Money, dueDate: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("") }
    var feeInput by remember { mutableStateOf("") }
    var dueDateIso by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val balance = Money.parse(balanceInput)
    val fee = Money.parse(feeInput)
    val netReceived = if (balance != null && fee != null) balance - fee else null

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
                    label = { Text("Amount you owe (₱)") },
                    isError = error != null,
                    supportingText = error?.let { errorText -> { Text(errorText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = feeInput,
                    onValueChange = { feeInput = it },
                    label = { Text("Fee deducted upfront, optional") },
                    placeholder = { Text("e.g. 60 if they gave you less than you owe") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                if (netReceived != null && fee != null && !fee.isZero) {
                    Text(
                        text = "You'll actually receive: ${netReceived.formatPhp()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = OceanTeal,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Interest rate %, optional") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                DueDateField(
                    isoDate = dueDateIso,
                    onDateSelected = { dueDateIso = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (balance == null || balance.isZero || balance.isNegative) {
                    error = "Enter a valid balance"
                    haptics.onValidationError()
                } else if (label.isBlank()) {
                    error = "Give it a name"
                    haptics.onValidationError()
                } else if (fee != null && (fee.isNegative || fee.minorUnits > balance.minorUnits)) {
                    error = "Fee can't be more than the amount you owe"
                    haptics.onValidationError()
                } else {
                    val rate = rateInput.toDoubleOrNull()
                    onConfirm(label, balance, rate, fee ?: Money.ZERO, dueDateIso.takeIf { it.isNotBlank() })
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
