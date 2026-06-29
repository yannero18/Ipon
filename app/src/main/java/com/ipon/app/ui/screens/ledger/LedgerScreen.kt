package com.ipon.app.ui.screens.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.Transaction
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.BalanceCard
import com.ipon.app.ui.components.TransactionCoinFab
import com.ipon.app.ui.components.TransactionRow
import com.ipon.app.ui.screens.reflection.DailyReflectionCard
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun LedgerScreen(
    viewModelFactory: IponViewModelFactory,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (transactionId: String) -> Unit,
    recentlyAddedId: String? = null
) {
    val viewModel: LedgerViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = RicePaper,
        floatingActionButton = {
            TransactionCoinFab(onClick = onAddTransactionClick)
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "MAGANDANG UMAGA",
                    style = MaterialTheme.typography.labelSmall,
                    color = KapeBrownSoft
                )
                Text(
                    text = "Your ledger",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                BalanceCard(
                    availableThisMonth = uiState.summary.net,
                    income = uiState.summary.income,
                    expense = uiState.summary.expense,
                    estimatedDaysOfRunway = uiState.estimatedDaysOfRunway,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                DailyReflectionCard(
                    viewModelFactory = viewModelFactory,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent activity",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelSmall,
                        color = OceanTeal
                    )
                }
            }

            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No entries yet this month.\nTap the coin to log your first transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KapeBrownSoft
                        )
                    }
                }
            }

            groupByDay(uiState.transactions).forEach { (dayLabel, dayTransactions) ->
                item {
                    Text(
                        text = dayLabel.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                    )
                }
                items(dayTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        isNewlyAdded = transaction.id == recentlyAddedId,
                        onClick = { onTransactionClick(transaction.id) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            item { Box(modifier = Modifier.padding(bottom = 80.dp)) }
        }
    }
}

private fun groupByDay(transactions: List<Transaction>): List<Pair<String, List<Transaction>>> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dateFormat = SimpleDateFormat("MMMM d", Locale.US)

    return transactions
        .groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.occurredAtEpochMillis }
            when {
                isSameDay(cal, today) -> "Today"
                isSameDay(cal, yesterday) -> "Yesterday"
                else -> dateFormat.format(tx.occurredAtEpochMillis)
            }
        }
        .toList()
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
