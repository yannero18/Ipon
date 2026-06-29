package com.ipon.app.ui.screens.recurring

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.TransactionCategory
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTemplateScreen(
    viewModelFactory: IponViewModelFactory,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: RecurringViewModel = viewModel(factory = viewModelFactory)

    var label by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var merchantRaw by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var dayOfPeriod by remember { mutableStateOf("1") }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("New recurring template", style = MaterialTheme.typography.titleMedium) },
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
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                placeholder = { Text("e.g. Rent, Meralco, Sweldo") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14000000))
                    .padding(4.dp)
            ) {
                ToggleOption(
                    label = "Expense",
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE; selectedCategory = null },
                    modifier = Modifier.weight(1f)
                )
                ToggleOption(
                    label = "Income",
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME; selectedCategory = null },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it; amountError = null },
                label = { Text("Amount (\u20b1)") },
                isError = amountError != null,
                supportingText = amountError?.let { errorText -> { Text(errorText) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            val categories: List<TransactionCategory> = when (type) {
                TransactionType.EXPENSE -> ExpenseCategory.entries.toList()
                TransactionType.INCOME -> IncomeCategory.entries.toList()
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    CategoryLabel(
                        category = category,
                        textColor = if (selectedCategory == category) RicePaper else KapeBrown,
                        tint = if (selectedCategory == category) RicePaper else KapeBrown,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(IponShapes.SquircleSm)
                            .background(if (selectedCategory == category) OceanTeal else Color.White)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = merchantRaw,
                onValueChange = { merchantRaw = it },
                label = { Text("Merchant / source (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Text(
                text = "Frequency",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14000000))
                    .padding(4.dp)
            ) {
                ToggleOption(
                    label = "Monthly",
                    selected = frequency == RecurrenceFrequency.MONTHLY,
                    onClick = { frequency = RecurrenceFrequency.MONTHLY; dayOfPeriod = "1" },
                    modifier = Modifier.weight(1f)
                )
                ToggleOption(
                    label = "Weekly",
                    selected = frequency == RecurrenceFrequency.WEEKLY,
                    onClick = { frequency = RecurrenceFrequency.WEEKLY; dayOfPeriod = "1" },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = dayOfPeriod,
                onValueChange = { dayOfPeriod = it },
                label = {
                    Text(
                        if (frequency == RecurrenceFrequency.MONTHLY) "Day of month (1-31)"
                        else "Day of week (1=Mon..7=Sun)"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )

            Button(
                onClick = {
                    val amount = Money.parse(amountInput)
                    val day = dayOfPeriod.toIntOrNull()
                    val category = selectedCategory

                    if (amount == null || amount.isZero) {
                        amountError = "Enter a valid amount"
                        return@Button
                    }
                    if (category == null || day == null || label.isBlank()) {
                        return@Button
                    }

                    viewModel.createTemplate(
                        label = label,
                        amount = amount,
                        type = type,
                        category = category,
                        merchantRaw = merchantRaw.ifBlank { null },
                        frequency = frequency,
                        dayOfPeriod = day.coerceIn(1, 31)
                    )
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
                shape = IponShapes.SquircleSm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Save template", color = RicePaper)
            }
        }
    }
}

@Composable
private fun ToggleOption(
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
