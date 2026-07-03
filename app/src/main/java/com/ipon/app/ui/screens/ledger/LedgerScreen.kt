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

    val context = LocalContext.current
    val haptics = remember(context) { com.ipon.app.util.HapticFeedbackManager(context) }

    Scaffold(
        containerColor = RicePaper
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    // Row for Greeting and Local Date separated cleanly
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = OceanTeal,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = KapeBrownSoft,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    // Vertical Spacer between greeting/date and the title row
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Row for "Dashboard" title and the top-right outlined action slot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown
                        )
                        
                        // Dynamic vector placeholder slot on the top-right corner for a notification bell or profile action to balance out the layout
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { /* Action placeholder */ },
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(1.dp, HairlineBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = KapeBrown,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Actionable Spending Power Hero Card (Refactored to AVAILABLE TO SPEND)
            item {
                val totalCap = uiState.totalEnvelopeCaps.minorUnits
                val remaining = uiState.remainingBudget.minorUnits
                val budgetProgressFraction = if (totalCap > 0L) {
                    (remaining.toFloat() / totalCap.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(IponShapes.SquircleLg)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(OceanTeal, Color(0xFF0C3841))
                            )
                        )
                        .drawBehind {
                            val h = this.size.height
                            val w = this.size.width
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, h * 0.82f)
                                cubicTo(
                                    w * 0.25f, h * 0.9f,
                                    w * 0.45f, h * 0.55f,
                                    w * 0.7f, h * 0.65f
                                )
                                cubicTo(
                                    w * 0.85f, h * 0.7f,
                                    w * 0.95f, h * 0.38f,
                                    w, h * 0.42f
                                )
                            }
                            this.drawPath(
                                path = path,
                                color = Color(0x1F34D399), // Neon mint/teal glow
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                            )
                            this.drawPath(
                                path = path,
                                color = Color(0x3BFFFFFF), // Crisp clean overlay
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        },
                    shape = IponShapes.SquircleLg,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "AVAILABLE TO SPEND",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = RicePaper.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = uiState.remainingBudget.formatPhp(),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ).merge(TabularSerifNumberStyle),
                                    color = RicePaper,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            // Remaining Budget Percentage Pill
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(RicePaper.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${(budgetProgressFraction * 100).toInt()}% Left",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RicePaper
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sleek, ultra-thin linear progress bar of remaining budget
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(RicePaper.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = budgetProgressFraction)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.remainingBudget.formatPhp()} left of ${uiState.totalEnvelopeCaps.formatPhp()} caps",
                                style = MaterialTheme.typography.bodySmall,
                                color = RicePaper.copy(alpha = 0.75f)
                            )
                            
                            val paydayPacing = remember(uiState.remainingBudget) {
                                val calendar = Calendar.getInstance()
                                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                                val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val (daysRemaining, label) = if (currentDay < 15) {
                                    (15 - currentDay) to "15th"
                                } else {
                                    (maxDays - currentDay + 1) to "1st"
                                }
                                val days = daysRemaining.coerceAtLeast(1)
                                val safeSpendMinor = uiState.remainingBudget.minorUnits / days
                                val safeSpend = Money.ofMinorUnits(safeSpendMinor.coerceAtLeast(0L))
                                safeSpend to "$days ${if (days == 1) "day" else "days"} to $label"
                            }
                            
                            Text(
                                text = "${paydayPacing.first.formatPhp()} safe/day (${paydayPacing.second})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF34D399)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = RicePaper.copy(alpha = 0.15f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Low-profile secondary row at the bottom of the card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL SAVINGS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = RicePaper.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = uiState.totalSavingsBalance.formatPhp(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RicePaper
                                )
                            }

                            // Show active goal if exists, otherwise show month's total contribution
                            val primaryGoal = uiState.activeGoals.firstOrNull()
                            if (primaryGoal != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "GOAL: ${primaryGoal.goal.label.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = RicePaper.copy(alpha = 0.6f)
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Flag,
                                            contentDescription = null,
                                            tint = RicePaper.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${((primaryGoal.fraction) * 100).toInt()}% Saved",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF34D399)
                                        )
                                        TextButton(
                                            onClick = { contributingToGoal = primaryGoal },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF34D399)),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("+ Deposit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "MONTH CONTRIBUTIONS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = RicePaper.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "+${uiState.monthlyContributionsSum.formatPhp()}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 Brand New Dashboard Summary (Pocket Money, Savings, Outflow/Inflow progress)
            item {
                DashboardSummary(
                    piggyBankBalance = uiState.availableBalance,
                    totalSavingsBalance = uiState.totalSavingsBalance,
                    totalIncome = uiState.summary.income,
                    totalExpense = uiState.summary.expense,
                    modifier = Modifier.fillMaxWidth()
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

            // 3. Envelope Budget Allocation Card (Tailwind Style)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = IponShapes.SquircleLg,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            icon = Icons.Outlined.ArrowDownward,
                            bgColor = RicePaperDeep,
                            onClick = { showQuickDepositDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCircleButton(
                            label = "Withdraw",
                            icon = Icons.Outlined.ArrowUpward,
                            bgColor = RicePaperDeep,
                            onClick = { showQuickWithdrawalDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCircleButton(
                            label = "Goal Save",
                            icon = Icons.Outlined.StarBorder,
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
                            icon = Icons.Outlined.Folder,
                            bgColor = RicePaperDeep,
                            onClick = {
                                editingEnvelopeCategory = ExpenseCategory.BILLS
                            },
                            modifier = Modifier.weight(1f)
                        )
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
                            text = "Transaction Ledger",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown
                        )
                        Text(
                            text = "${filteredTransactions.size} logs found",
                            style = MaterialTheme.typography.bodySmall,
                            color = KapeBrownSoft
                        )
                    }
                    TextButton(onClick = onAddTransactionClick) {
                        Text("+ Add Entry", color = OceanTeal, style = MaterialTheme.typography.labelLarge)
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
                                FilterChip(
                                    selected = selectedCategoryFilter == cat,
                                    onClick = { selectedCategoryFilter = cat },
                                    label = { Text(cat) },
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

            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(IponShapes.SquircleLg)
                            .background(Color.White)
                            .border(1.dp, HairlineBorder, IponShapes.SquircleLg)
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
                items(filteredTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        isNewlyAdded = transaction.id == recentlyAddedId,
                        onClick = { onTransactionClick(transaction.id) },
                        onDeleteClick = { transactionToDelete = transaction },
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
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
                                        Icon(
                                            imageVector = category.icon(),
                                            contentDescription = null,
                                            tint = if (selectedCategory == category) OceanTeal else KapeBrownSoft,
                                            modifier = Modifier.size(16.dp)
                                        )
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
                                        Icon(
                                            imageVector = category.icon(),
                                            contentDescription = null,
                                            tint = if (selectedCategory == category) OceanTeal else KapeBrownSoft,
                                            modifier = Modifier.size(16.dp)
                                        )
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
}

@Composable
private fun QuickActionCircleButton(
    label: String,
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = OceanTeal,
                modifier = Modifier.size(22.dp)
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
            // Category Icon inside squircle chip
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (envelope?.customIcon != null && envelope.customIcon.isNotEmpty()) {
                    Text(
                        text = envelope.customIcon,
                        fontSize = 18.sp
                    )
                } else {
                    Icon(
                        imageVector = category.icon(),
                        contentDescription = categoryName,
                        tint = OceanTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
