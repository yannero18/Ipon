package com.ipon.app.ui.screens.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.model.EnvelopeProgress
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.IncomeCategory
import com.ipon.app.data.model.GoalProgress
import com.ipon.app.data.model.Transaction
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.components.TransactionRow
import com.ipon.app.ui.components.TransactionCoinFab
import com.ipon.app.ui.components.InteractiveEnvelopeDonutChart
import com.ipon.app.ui.components.DashboardSummary
import com.ipon.app.ui.components.BalanceCard
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.*
import com.ipon.app.util.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModelFactory: IponViewModelFactory,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    recentlyAddedId: String? = null
) {
    val viewModel: LedgerViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var showQuickDepositDialog by remember { mutableStateOf(false) }
    var showQuickWithdrawalDialog by remember { mutableStateOf(false) }
    var contributingToGoal by remember { mutableStateOf<GoalProgress?>(null) }
    var editingEnvelopeCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showNoGoalsDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    var showPaydayDialog by remember { mutableStateOf(false) }
    var paydaysInput by remember { mutableStateOf("") }

    val filteredTransactions = remember(uiState.transactions, searchQuery, selectedTypeFilter, selectedCategoryFilter) {
        uiState.transactions.filter { tx ->
            val matchesSearch = searchQuery.isBlank() || 
                tx.category.contains(searchQuery, ignoreCase = true) ||
                (tx.merchantRaw ?: "").contains(searchQuery, ignoreCase = true) ||
                (tx.note ?: "").contains(searchQuery, ignoreCase = true)

            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter

            val matchesCategory = selectedCategoryFilter == null || tx.category.equals(selectedCategoryFilter, ignoreCase = true)

            matchesSearch && matchesType && matchesCategory
        }
    }

    val presentCategories = remember(uiState.transactions) {
        uiState.transactions.map { it.category }.distinct().sorted()
    }

    val groupedTransactions = remember(filteredTransactions) {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val formatFull = SimpleDateFormat("EEEE, MMM d", Locale.US)
        
        filteredTransactions.groupBy { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.occurredAtEpochMillis }
            when {
                txCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                txCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "TODAY"
                
                txCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                txCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "YESTERDAY"
                
                else -> formatFull.format(tx.occurredAtEpochMillis).uppercase(Locale.US)
            }
        }.map { (title, items) ->
            title to items
        }
    }

    val context = LocalContext.current
    val haptics = remember(context) { com.ipon.app.util.HapticFeedbackManager(context) }

    Scaffold(
        containerColor = RicePaper
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header & Greeting
            item {
                val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                val greeting = remember(currentHour) {
                    when (currentHour) {
                        in 0..11 -> "MAGANDANG UMAGA"
                        in 12..17 -> "MAGANDANG HAPON"
                        else -> "MAGANDANG GABI"
                    }
                }
                val formattedDate = remember {
                    SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US).format(Calendar.getInstance().time)
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 0.dp)
                ) {
                    // Row for Greeting and Local Date separated cleanly
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = KapeBrownSoft
                    )
                    
                    // Vertical Spacer between greeting/date and the title row
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Row for "Dashboard" title and the top-right outlined action slot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your ledger",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown
                        )
                        
                        // The Clean 3-Dots Menu from the Mockup!
                        IconButton(
                            onClick = { 
                                paydaysInput = uiState.currentPaydays.joinToString(", ")
                                showPaydayDialog = true 
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Payday Settings",
                                tint = KapeBrown
                            )
                        }
                    }
                }
            }

            // 2. Actionable Spending Power Hero Card (Refactored to AVAILABLE TO SPEND)
            item {
                BalanceCard(
                    availableThisMonth = uiState.summary.net,
                    income = uiState.summary.income,
                    expense = uiState.summary.expense,
                    totalSavings = uiState.totalSavingsBalance,
                    estimatedDaysOfRunway = uiState.estimatedDaysOfRunway,
                    accountName = uiState.accountName,
                    daysUntilPayday = uiState.daysUntilPayday,
                    safeDailySpend = uiState.safeDailySpend
                )
            }

            // Automation Banner: Auto-log due recurring bills in one tap
            if (uiState.dueTemplates.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HairlineBorder, IponShapes.SquircleLg),
                        shape = IponShapes.SquircleLg,
                        colors = CardDefaults.cardColors(containerColor = RicePaperDeep),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "IPON AUTOMATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OceanTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You have ${uiState.dueTemplates.size} recurring logs due. Click to automatically post them to your ledger.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KapeBrown
                                )
                            }
                            Button(
                                onClick = { viewModel.autoConfirmAllDueTemplates() },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                                shape = IponShapes.SquircleSm
                            ) {
                                Text("Auto-Log", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 5. Interactive Transaction Log Component with local Search & Horizontal Filters
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent activity",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FrauncesFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = KapeBrown
                        )
                        Text(
                            text = "${filteredTransactions.size} logs found",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft
                        )
                    }
                    TextButton(onClick = onAddTransactionClick) {
                        Text(
                            text = "See all",
                            color = OceanTeal,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Search Bar Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...", color = KapeBrownSoft.copy(alpha = 0.5f)) },
                    leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", color = KapeBrownSoft, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input"),
                    shape = IponShapes.SquircleMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = OceanTeal,
                        unfocusedBorderColor = KapeBrownSoft.copy(alpha = 0.15f),
                        cursorColor = OceanTeal
                    ),
                    singleLine = true
                )
            }

            // Type Filters: All, Expenses, Incomes
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Type:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrownSoft
                    )
                    
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OceanTeal,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = KapeBrownSoft
                        )
                    )
                    
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                        label = { Text("Expenses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OceanTeal,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = KapeBrownSoft
                        )
                    )
                    
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = { selectedTypeFilter = TransactionType.INCOME },
                        label = { Text("Incomes") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OceanTeal,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = KapeBrownSoft
                        )
                    )
                }
            }

            // Category scrollable filter chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrownSoft
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == null,
                                    onClick = { selectedCategoryFilter = null },
                                    label = { Text("All Categories") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OceanTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = KapeBrownSoft
                                    )
                                )
                            }
                            
                            items(presentCategories) { cat ->
                                val categoryEnum = ExpenseCategory.entries.find { it.displayName == cat } 
                                    ?: IncomeCategory.entries.find { it.displayName == cat }

                                FilterChip(
                                    selected = selectedCategoryFilter == cat,
                                    onClick = { selectedCategoryFilter = cat },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (categoryEnum != null) {
                                                Text(categoryEnum.emoji, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(cat)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OceanTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = KapeBrownSoft
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(IponShapes.SquircleLg)
                            .background(WarmCream)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🪙", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Start fresh from 0!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = KapeBrown
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No logs in your ledger yet. Press the orange '+' button below or '+ Add Entry' to track your very first cash in or out!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KapeBrownSoft,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(IponShapes.SquircleLg)
                            .background(WarmCream)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions logged matching filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KapeBrownSoft,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedTransactions.forEach { (title, groupItems) ->
                    item {
                        Text(
                            text = title.uppercase(java.util.Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(groupItems, key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            isNewlyAdded = transaction.id == recentlyAddedId,
                            onClick = { onTransactionClick(transaction.id) },
                            onDeleteClick = { transactionToDelete = transaction },
                            hasCardContainer = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Divider or spacing between transactions and supplementary blocks
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. Envelope Budget Allocation Card (Tailwind Style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IponShapes.SquircleLg,
                    colors = CardDefaults.cardColors(containerColor = WarmCream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ENVELOPE BUDGETING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KapeBrownSoft,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Monthly Allocations",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = KapeBrown
                                )
                            }
                            // Available to Allocate Badge
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Available Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KapeBrownSoft
                                )
                                Text(
                                    text = uiState.availableBalance.formatPhp(),
                                    style = MaterialTheme.typography.bodyLarge.merge(TabularSerifNumberStyle),
                                    color = OceanTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Donut Chart of allocations
                        InteractiveEnvelopeDonutChart(envelopes = uiState.envelopes)

                        Spacer(modifier = Modifier.height(20.dp))

                        // We render dynamic allocation rows for: 'Rent' (Bills), 'Food' (Food), and 'Savings' (Other / General Savings)
                        val rentEnvelope = uiState.envelopes.find { it.category == ExpenseCategory.BILLS }
                        val foodEnvelope = uiState.envelopes.find { it.category == ExpenseCategory.FOOD }
                        val savingsEnvelope = uiState.envelopes.find { it.category == ExpenseCategory.OTHER }

                        EnvelopeAllocationRow(
                            categoryName = "Rent & Bills",
                            category = ExpenseCategory.BILLS,
                            envelope = rentEnvelope,
                            onClick = { editingEnvelopeCategory = ExpenseCategory.BILLS }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        EnvelopeAllocationRow(
                            categoryName = "Food & Meals",
                            category = ExpenseCategory.FOOD,
                            envelope = foodEnvelope,
                            onClick = { editingEnvelopeCategory = ExpenseCategory.FOOD }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        EnvelopeAllocationRow(
                            categoryName = "Savings / Other",
                            category = ExpenseCategory.OTHER,
                            envelope = savingsEnvelope,
                            onClick = { editingEnvelopeCategory = ExpenseCategory.OTHER }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Tap any category cap above to adjust its allocated amount. Balance updates in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft
                        )
                    }
                }
            }

            // 4. Quick Actions Panel (Interactive Actions Deck)
            item {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrown,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(IponShapes.SquircleLg)
                            .background(Color.White)
                            .border(1.dp, HairlineBorder, IponShapes.SquircleLg)
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickActionCircleButton(
                            label = "Deposit",
                            emoji = "📥",
                            bgColor = RicePaperDeep,
                            onClick = { showQuickDepositDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCircleButton(
                            label = "Withdraw",
                            emoji = "💸",
                            bgColor = RicePaperDeep,
                            onClick = { showQuickWithdrawalDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCircleButton(
                            label = "Goal Save",
                            emoji = "🌟",
                            bgColor = RicePaperDeep,
                            onClick = {
                                val primary = uiState.activeGoals.firstOrNull()
                                if (primary != null) {
                                    contributingToGoal = primary
                                } else {
                                    showNoGoalsDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCircleButton(
                            label = "Budget",
                            emoji = "💼",
                            bgColor = RicePaperDeep,
                            onClick = {
                                editingEnvelopeCategory = ExpenseCategory.BILLS
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // --- QUICK ACTION DIALOGS ---

    // 1. Quick Deposit Dialog
    if (showQuickDepositDialog) {
        var selectedAmount by remember { mutableStateOf<Money?>(null) }
        var selectedCategory by remember { mutableStateOf(IncomeCategory.SALARY) }
        var labelInput by remember { mutableStateOf("") }
        var customAmountInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickDepositDialog = false },
            title = { Text("Log Quick Deposit", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a preset amount or enter a custom value:", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
                    
                    // Presets
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(500L, 1000L, 5000L).forEach { amountPesos ->
                            val amount = Money.ofMinorUnits(amountPesos * 100)
                            FilterChip(
                                selected = selectedAmount == amount,
                                onClick = { 
                                    selectedAmount = amount
                                    customAmountInput = ""
                                },
                                label = { Text(amount.formatPhp()) }
                            )
                        }
                    }

                    // Custom Amount Input
                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { 
                            customAmountInput = it
                            selectedAmount = Money.parse(it)
                        },
                        label = { Text("Custom Amount (₱)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Source Category:", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        IncomeCategory.entries.take(3).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(category.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(category.displayName)
                                    }
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Optional Label (e.g. Sweldo)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalAmount = selectedAmount
                        if (finalAmount != null && finalAmount.isPositive) {
                            viewModel.addQuickTransaction(
                                type = TransactionType.INCOME,
                                amount = finalAmount,
                                categoryName = selectedCategory.displayName,
                                label = labelInput.ifBlank { "Quick Deposit" }
                            )
                            showQuickDepositDialog = false
                        }
                    }
                ) {
                    Text("Deposit", color = OceanTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickDepositDialog = false }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            }
        )
    }

    // 2. Quick Withdrawal Dialog
    if (showQuickWithdrawalDialog) {
        var selectedAmount by remember { mutableStateOf<Money?>(null) }
        var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
        var labelInput by remember { mutableStateOf("") }
        var customAmountInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickWithdrawalDialog = false },
            title = { Text("Log Quick Withdrawal", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a preset amount or enter a custom value:", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
                    
                    // Presets
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(100L, 200L, 500L).forEach { amountPesos ->
                            val amount = Money.ofMinorUnits(amountPesos * 100)
                            FilterChip(
                                selected = selectedAmount == amount,
                                onClick = { 
                                    selectedAmount = amount
                                    customAmountInput = ""
                                },
                                label = { Text(amount.formatPhp()) }
                            )
                        }
                    }

                    // Custom Amount Input
                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { 
                            customAmountInput = it
                            selectedAmount = Money.parse(it)
                        },
                        label = { Text("Custom Amount (₱)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Expense Category:", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPO, ExpenseCategory.BILLS).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(category.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(category.displayName)
                                    }
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Optional Label (e.g. Lunch)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalAmount = selectedAmount
                        if (finalAmount != null && finalAmount.isPositive) {
                            viewModel.addQuickTransaction(
                                type = TransactionType.EXPENSE,
                                amount = finalAmount,
                                categoryName = selectedCategory.displayName,
                                label = labelInput.ifBlank { "Quick Withdrawal" }
                            )
                            showQuickWithdrawalDialog = false
                        }
                    }
                ) {
                    Text("Withdraw", color = Terracotta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickWithdrawalDialog = false }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            }
        )
    }

    // 3. Goal Contribution Dialog
    contributingToGoal?.let { goalProgress ->
        var amountInput by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { contributingToGoal = null },
            title = { Text("Contribute to ${goalProgress.goal.label}", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add money to this savings goal. Total savings progress will increase immediately.", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it; error = null },
                        label = { Text("Amount (₱)") },
                        isError = error != null,
                        supportingText = error?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = Money.parse(amountInput)
                        if (parsed == null || !parsed.isPositive) {
                            error = "Enter a valid amount"
                            haptics.onValidationError()
                        } else {
                            viewModel.addQuickGoalContribution(goalProgress.goal.id, parsed)
                            haptics.onTransactionSaved(parsed, Money.ZERO)
                            contributingToGoal = null
                        }
                    }
                ) {
                    Text("Contribute", color = OceanTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contributingToGoal = null }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            }
        )
    }

    // 4. Envelope Cap Allocation Dialog
    editingEnvelopeCategory?.let { category ->
        val envelope = uiState.envelopes.find { it.category == category }
        var amountInput by remember {
            mutableStateOf(
                envelope?.let { (it.cap.minorUnits / 100.0).toString() } ?: ""
            )
        }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingEnvelopeCategory = null },
            title = { Text("Allocate to ${category.displayName}", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set the spending cap allocation for this envelope. This directly updates the available balance to allocate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it; error = null },
                        label = { Text("Allocated Amount (₱)") },
                        isError = error != null,
                        supportingText = error?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = Money.parse(amountInput)
                        if (parsed == null || parsed.isNegative) {
                            error = "Enter a valid positive amount"
                            haptics.onValidationError()
                        } else {
                            viewModel.setEnvelopeCap(category, parsed)
                            editingEnvelopeCategory = null
                        }
                    }
                ) {
                    Text("Allocate", color = OceanTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (envelope != null) {
                        TextButton(
                            onClick = {
                                haptics.onDeleteConfirmed()
                                viewModel.removeEnvelopeCap(category)
                                editingEnvelopeCategory = null
                            }
                        ) {
                            Text("Remove", color = Terracotta)
                        }
                    }
                    TextButton(onClick = { editingEnvelopeCategory = null }) {
                        Text("Cancel", color = KapeBrownSoft)
                    }
                }
            }
        )
    }

    // 5. No Active Goals AlertDialog
    if (showNoGoalsDialog) {
        AlertDialog(
            onDismissRequest = { showNoGoalsDialog = false },
            title = { Text("No Active Savings Goals", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "You don't have any savings goals set up yet. To start contributing, please head to the 'Plan' tab and define your first goal!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KapeBrownSoft
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNoGoalsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }

    // 6. Delete Transaction Confirmation Dialog
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Entry?", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this ${tx.type.name.lowercase(Locale.US)} entry of ${tx.amount.formatPhp()} under '${tx.category}'?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KapeBrownSoft
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.onDeleteConfirmed()
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel", color = KapeBrownSoft)
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }
    
    // 7. NEW PAYDAY SCHEDULE SETTINGS DIALOG
    if (showPaydayDialog) {
        AlertDialog(
            onDismissRequest = { showPaydayDialog = false },
            title = { Text("Set Payday Schedule", color = KapeBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the days of the month you get paid, separated by commas.", style = MaterialTheme.typography.bodyMedium, color = KapeBrownSoft)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = paydaysInput,
                        onValueChange = { paydaysInput = it },
                        label = { Text("Paydays") },
                        placeholder = { Text("e.g. 15, 30") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val days = paydaysInput.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..31 }
                    if (days.isNotEmpty()) {
                        viewModel.updatePaydays(days)
                    }
                    showPaydayDialog = false
                }) {
                    Text("Save", color = OceanTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaydayDialog = false }) { Text("Cancel", color = KapeBrownSoft) }
            },
            containerColor = RicePaper
        )
    }
}

@Composable
private fun QuickActionCircleButton(
    label: String,
    emoji: String,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(IponShapes.SquircleMd)
                .background(bgColor)
                .border(1.dp, HairlineBorder, IponShapes.SquircleMd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = KapeBrownSoft,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EnvelopeAllocationRow(
    categoryName: String,
    category: ExpenseCategory,
    envelope: EnvelopeProgress?,
    onClick: () -> Unit
) {
    val cap = envelope?.cap ?: Money.ZERO
    val spent = envelope?.spent ?: Money.ZERO
    val isOverBudget = envelope?.isOverBudget ?: false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleSm)
            .background(RicePaperDeep)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Category Emoji inside squircle chip
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val displayIcon = if (envelope?.customIcon != null && envelope.customIcon.isNotEmpty()) envelope.customIcon else category.emoji
                Text(
                    text = displayIcon,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = KapeBrown
                )
                Text(
                    text = "Spent: ${spent.formatPhp()} of ${cap.formatPhp()}",
                    style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                    color = if (isOverBudget) Terracotta else KapeBrownSoft
                )
            }
        }
        
        // Cap Tag
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (cap.isZero) Color(0xFFEEEEEE) else Color(0xFFE0F2F1))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (cap.isZero) "Not Set" else cap.formatPhp(),
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = if (cap.isZero) KapeBrownSoft else OceanTeal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}