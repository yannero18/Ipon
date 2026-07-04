package com.ipon.app.ui.screens.insights

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.CategoryTrend
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.SavingsSuggestion
import com.ipon.app.data.model.SuggestionSeverity
import com.ipon.app.data.model.TrendDirection
import com.ipon.app.data.model.CategorySlice
import com.ipon.app.data.model.Report
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.model.emoji
import com.ipon.app.util.Money
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.theme.HairlineBorder
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.TabularSerifNumberStyle
import com.ipon.app.ui.theme.MonoFamily
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.ui.theme.WarmCream
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class MoodSpendingPattern(
    val mood: MoodRating,
    val totalSpent: Money,
    val count: Int,
    val topCategory: String?
)

@Composable
fun InsightsScreen(
    viewModelFactory: IponViewModelFactory,
    onYearRecapClick: () -> Unit,
    onLogExpenseClick: () -> Unit
) {
    val viewModel: InsightsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val reports by viewModel.reports.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var activeReportTitle by remember { mutableStateOf<String?>(null) }
    var activeReportText by remember { mutableStateOf<String?>(null) }
    var sweepResultText by remember { mutableStateOf<String?>(null) }
    var recurringResultText by remember { mutableStateOf<String?>(null) }

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "MAGANDANG UMAGA"
            in 12..17 -> "MAGANDANG HAPON"
            else -> "MAGANDANG GABI"
        }
    }

    val topPattern = remember(uiState.reflections, uiState.monthTransactions) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val patternsList = uiState.reflections.map { reflection ->
            val txsOnDay = uiState.monthTransactions.filter { tx ->
                tx.type == com.ipon.app.data.local.TransactionType.EXPENSE &&
                        sdf.format(java.util.Date(tx.occurredAtEpochMillis)) == reflection.dayKey
            }
            reflection.mood to txsOnDay
        }.groupBy({ it.first }, { it.second })

        val moodStats = patternsList.map { (mood, txLists) ->
            val allTxs = txLists.flatten()
            val totalSpentMinor = allTxs.sumOf { it.amount.minorUnits }
            val count = allTxs.size
            val topCategory = allTxs.groupBy { it.category }
                .maxByOrNull { it.value.size }?.key

            MoodSpendingPattern(
                mood = mood,
                totalSpent = Money.ofMinorUnits(totalSpentMinor),
                count = count,
                topCategory = topCategory
            )
        }.filter { it.count > 0 }

        moodStats.maxByOrNull { it.totalSpent.minorUnits }
    }

    Scaffold(containerColor = RicePaper) { padding ->
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
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Insights",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Spent ${uiState.summary.expense.formatPhp()} this month",
                            style = MaterialTheme.typography.bodyLarge.merge(TabularSerifNumberStyle),
                            color = KapeBrownSoft
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OceanTeal.copy(alpha = 0.08f))
                            .clickable { onYearRecapClick() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Year in Ipon",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OceanTeal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- SMART AUTOMATION SECTION ---
            item {
                Text(
                    text = "SMART AUTOMATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Card 1: Sweep Envelope Excess
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(WarmCream)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sends unspent food/transpo balances directly to savings goals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KapeBrown,
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val sweeps = viewModel.sweepExcessFundsToGoals()
                                    sweepResultText = if (sweeps.isEmpty()) {
                                        "No excess envelope budgets were found to sweep, or you have no active goals."
                                    } else {
                                        "Sweep summary:\n\n" + sweeps.joinToString("\n\n")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sweep_excess_btn")
                        ) {
                            Text(
                                text = "Sweep",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Card 2: Process Recurring
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(WarmCream)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Process recurring transactions",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = KapeBrown
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Auto-log scheduled sweldo, monthly utilities, and drafts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KapeBrownSoft,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val count = viewModel.processDueRecurringTransactions()
                                    recurringResultText = if (count > 0) {
                                        "Successfully processed $count scheduled recurring transactions!"
                                    } else {
                                        "Your recurring templates are completely up to date for this cycle."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("trigger_recurring_btn")
                        ) {
                            Text(
                                text = "Process",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Card 3: Spending Report
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(WarmCream)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Generate Spending Report",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = KapeBrown
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Saves an offline digest tracking your budget progress.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KapeBrownSoft,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val report = viewModel.generateMonthlyReport()
                                    activeReportTitle = report.title
                                    activeReportText = report.content
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("generate_report_btn")
                        ) {
                            Text(
                                text = "Generate",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- MOOD & SPENDING PATTERNS ---
            if (topPattern != null) {
                item {
                    val moodLabel = when (topPattern.mood) {
                        MoodRating.GREAT -> "happy"
                        MoodRating.OKAY -> "calm"
                        MoodRating.STRESSED -> "stressed"
                        MoodRating.REGRET -> "regretful"
                    }

                    val adviceText = when (topPattern.mood) {
                        MoodRating.GREAT -> "It's great to see positive spending, but remember to stay within your envelope limits!"
                        MoodRating.OKAY -> "Consistent, calm spending helps you build solid long-term ipon habits."
                        MoodRating.STRESSED -> "Next time you feel stressed, take a deep breath or log a reflection to pause before stress-spending!"
                        MoodRating.REGRET -> "Reflecting on purchase regret is a powerful step towards more mindful spending. You got this!"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(WarmCream)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "MOOD & SPENDING PATTERNS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "You spent ${topPattern.totalSpent.formatPhp()} on ${topPattern.count} transactions while feeling $moodLabel. Your top spent category was ${topPattern.topCategory ?: "Other"}.",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                            color = KapeBrown,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFF9E6))
                                .border(1.dp, Color(0xFFF1E3C3), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "💡",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = adviceText,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = KapeBrown
                                )
                            }
                        }
                    }
                }
            }

            // --- TOP CATEGORIES THIS MONTH ---
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(WarmCream)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "TOP CATEGORIES THIS MONTH",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                            color = KapeBrownSoft,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        uiState.categoryBreakdown.forEachIndexed { index, slice ->
                            CategoryBreakdownRow(
                                slice = slice,
                                totalExpenseMinorUnits = uiState.summary.expense.minorUnits,
                                modifier = Modifier.padding(bottom = if (index == uiState.categoryBreakdown.lastIndex) 0.dp else 16.dp)
                            )
                        }
                    }
                }
            } else if (!uiState.isLoading) {
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
                            tint = KapeBrownSoft.copy(alpha = 0.35f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No expenses logged yet this month.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = KapeBrown,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start tracking your spending to see active insights.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KapeBrownSoft,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onLogExpenseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                            shape = IponShapes.SquircleMd,
                            modifier = Modifier.testTag("log_expense_empty_state_btn")
                        ) {
                            Text(
                                text = "Log an Expense",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- SAVED SPENDING REPORTS ---
            if (reports.isNotEmpty()) {
                item {
                    Text(
                        text = "SAVED SPENDING REPORTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                items(reports, key = { it.id }) { report ->
                    ReportRow(
                        report = report,
                        onViewClick = {
                            activeReportTitle = report.title
                            activeReportText = report.content
                        },
                        onDeleteClick = {
                            coroutineScope.launch {
                                viewModel.deleteReport(report.id)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            if (uiState.suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "WORTH A LOOK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                items(uiState.suggestions, key = { it.id }) { suggestion ->
                    SuggestionCard(suggestion = suggestion, modifier = Modifier.padding(bottom = 10.dp))
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (uiState.trends.isNotEmpty()) {
                item {
                    Text(
                        text = "VS LAST MONTH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                items(uiState.trends.take(3), key = { it.category }) { trend ->
                    TrendRow(trend = trend, modifier = Modifier.padding(bottom = 10.dp))
                }
                item { Spacer(modifier = Modifier.height(14.dp)) }
            }
        }
    }

    // --- RECURRING RESULT DIALOG ---
    recurringResultText?.let { text ->
        AlertDialog(
            onDismissRequest = { recurringResultText = null },
            confirmButton = {
                Button(
                    onClick = { recurringResultText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("Understood", color = RicePaper)
                }
            },
            title = {
                Text(
                    text = "Recurring Scheduler",
                    style = MaterialTheme.typography.titleMedium,
                    color = KapeBrown
                )
            },
            text = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KapeBrownSoft
                )
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }

    // --- SWEEP RESULT DIALOG ---
    sweepResultText?.let { text ->
        AlertDialog(
            onDismissRequest = { sweepResultText = null },
            confirmButton = {
                Button(
                    onClick = { sweepResultText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("Excellent", color = RicePaper)
                }
            },
            title = {
                Text(
                    text = "Envelope Savings Sweep",
                    style = MaterialTheme.typography.titleMedium,
                    color = KapeBrown
                )
            },
            text = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KapeBrownSoft
                )
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }

    // --- REPORT DETAILED VIEWER DIALOG ---
    if (activeReportText != null && activeReportTitle != null) {
        AlertDialog(
            onDismissRequest = {
                activeReportText = null
                activeReportTitle = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeReportText = null
                        activeReportTitle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleSm
                ) {
                    Text("Close", color = RicePaper)
                }
            },
            title = {
                Text(
                    text = activeReportTitle!!,
                    style = MaterialTheme.typography.titleLarge,
                    color = KapeBrown
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(IponShapes.SquircleSm)
                            .background(RicePaperDeep)
                            .border(1.dp, KapeBrownSoft.copy(alpha = 0.1f), IponShapes.SquircleSm)
                            .padding(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            item {
                                Text(
                                    text = activeReportText!!,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = MonoFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = KapeBrown
                                )
                            }
                        }
                    }
                }
            },
            containerColor = RicePaper,
            shape = IponShapes.SquircleLg
        )
    }
}

@Composable
private fun AutomationActionRow(
    title: String,
    subtitle: String,
    icon: Any, // Supporting either ImageVector or Custom SVG
    tint: Color,
    buttonTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleMd)
            .background(RicePaperDeep.copy(alpha = 0.4f))
            .border(1.dp, HairlineBorder, IponShapes.SquircleMd)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(buttonTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(IponShapes.SquircleSm)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = tint
                )
            } else {
                Icon(
                    imageVector = com.ipon.app.ui.icons.IponIcons.Alkansya,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = tint
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = KapeBrown
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KapeBrownSoft.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ReportRow(
    report: Report,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateString = remember(report.createdAtEpochMillis) {
        val format = SimpleDateFormat("MMMM d, yyyy · hh:mm a", Locale.US)
        format.format(Date(report.createdAtEpochMillis))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(IponShapes.SquircleMd)
            .background(WarmCream)
            .border(1.dp, KapeBrownSoft.copy(alpha = 0.15f), IponShapes.SquircleMd)
            .clickable(onClick = onViewClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(OceanTeal.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = OceanTeal
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = KapeBrown
            )
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.testTag("delete_report_${report.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete report",
                tint = Terracotta.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: SavingsSuggestion, modifier: Modifier = Modifier) {
    val accentColor = when (suggestion.severity) {
        SuggestionSeverity.IMPORTANT -> Terracotta
        SuggestionSeverity.NOTABLE -> JeepneyOrange
        SuggestionSeverity.INFO -> OceanTeal
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(WarmCream)
            .border(1.dp, accentColor.copy(alpha = 0.3f), IponShapes.SquircleLg)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyLarge,
                color = KapeBrown,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = suggestion.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp)
        )
    }
}

@Composable
private fun TrendRow(trend: CategoryTrend, modifier: Modifier = Modifier) {
    val percent = trend.percentChange
    val description = when {
        percent == null && trend.lastMonth.isZero && !trend.thisMonth.isZero ->
            "New this month"
        percent == null -> "No change"
        percent == 0 -> "Same as last month"
        percent > 0 -> "Up $percent% from last month"
        else -> "Down ${-percent}% from last month"
    }
    val descriptionColor = when (trend.direction) {
        TrendDirection.UP -> Terracotta
        TrendDirection.DOWN -> JeepneyOrange
        TrendDirection.FLAT -> KapeBrownSoft
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryLabel(category = trend.category, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
            color = descriptionColor
        )
    }
}

@Composable
private fun CategoryBreakdownRow(
    slice: CategorySlice,
    totalExpenseMinorUnits: Long,
    modifier: Modifier = Modifier
) {
    val category = ExpenseCategory.fromDisplayName(slice.category)

    val percent: Int = if (totalExpenseMinorUnits == 0L) {
        0
    } else {
        BigDecimal(slice.total.minorUnits)
            .divide(BigDecimal(totalExpenseMinorUnits), 4, RoundingMode.HALF_EVEN)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_EVEN)
            .toInt()
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CategoryLabel(category = category, text = slice.category)
            Text(
                text = slice.total.formatPhp(),
                style = MaterialTheme.typography.bodyLarge.merge(TabularSerifNumberStyle),
                color = KapeBrown
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RicePaperDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(com.ipon.app.ui.components.getEnvelopeColor(category))
            )
        }
        Text(
            text = "$percent% of spending",
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
