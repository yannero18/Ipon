package com.ipon.app.ui.screens.budgetplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.ipon.app.data.model.FiftyThirtyTwentyComparison
import com.ipon.app.data.model.ZeroBasedBudget
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import com.ipon.app.util.Money

private enum class FrameworkTab { FIFTY_THIRTY_TWENTY, ZERO_BASED }

@Composable
fun BudgetPlanScreen(viewModelFactory: IponViewModelFactory) {
    val viewModel: BudgetPlanViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(FrameworkTab.FIFTY_THIRTY_TWENTY) }

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Budget plan",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Two ways to look at this month's income",
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
                    TabOption(
                        label = "50/30/20",
                        selected = selectedTab == FrameworkTab.FIFTY_THIRTY_TWENTY,
                        onClick = { selectedTab = FrameworkTab.FIFTY_THIRTY_TWENTY },
                        modifier = Modifier.weight(1f)
                    )
                    TabOption(
                        label = "Zero-based",
                        selected = selectedTab == FrameworkTab.ZERO_BASED,
                        onClick = { selectedTab = FrameworkTab.ZERO_BASED },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when (selectedTab) {
                FrameworkTab.FIFTY_THIRTY_TWENTY -> {
                    uiState.fiftyThirtyTwenty?.let { comparison ->
                        item {
                            FiftyThirtyTwentyContent(
                                comparison = comparison,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        }
                    }
                }
                FrameworkTab.ZERO_BASED -> {
                    uiState.zeroBased?.let { budget ->
                        item {
                            ZeroBasedContent(
                                budget = budget,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
private fun FiftyThirtyTwentyContent(comparison: FiftyThirtyTwentyComparison, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (comparison.income.isZero) {
            Text(
                text = "Log some income this month to see how it compares to the 50/30/20 framework.",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
            return@Column
        }

        Text(
            text = "Based on ${comparison.income.formatPhp()} in income this month. " +
                "50% needs, 30% wants, 20% savings/debt -- a classic rule of thumb, not a rule this app enforces.",
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(bottom = 18.dp)
        )

        BucketBar(
            label = "Needs",
            actual = comparison.needsActual,
            target = comparison.needsTarget,
            isOver = comparison.needsOverTarget,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        BucketBar(
            label = "Wants",
            actual = comparison.wantsActual,
            target = comparison.wantsTarget,
            isOver = comparison.wantsOverTarget,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        BucketBar(
            label = "Savings / debt",
            actual = comparison.savingsActual,
            target = comparison.savingsTarget,
            isOver = comparison.savingsUnderTarget,
            invertOverColor = true,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun BucketBar(
    label: String,
    actual: Money,
    target: Money,
    isOver: Boolean,
    invertOverColor: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barColor = if (isOver) Terracotta else OceanTeal
    val fraction = if (target.isZero) 0f else (actual.minorUnits.toFloat() / target.minorUnits.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = KapeBrown)
            Text(
                text = "${actual.formatPhp()} / ${target.formatPhp()}",
                style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                color = KapeBrownSoft
            )
        }
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
                    .fillMaxWidth(fraction = fraction)
                    .height(10.dp)
                    .clip(IponShapes.SquircleSm)
                    .background(barColor)
            )
        }
        if (isOver) {
            Text(
                text = if (invertOverColor) "Below target" else "Over target",
                style = MaterialTheme.typography.bodyMedium,
                color = Terracotta,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ZeroBasedContent(budget: ZeroBasedBudget, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (budget.income.isZero) {
            Text(
                text = "Log some income this month to see where it's been assigned.",
                style = MaterialTheme.typography.bodyMedium,
                color = KapeBrownSoft
            )
            return@Column
        }

        Text(
            text = "Every peso gets a job. This compares your income against what you've " +
                "already assigned to envelopes and active recurring expenses.",
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(bottom = 18.dp)
        )

        AssignmentRow(label = "Income", value = budget.income.formatPhp(), color = OceanTeal)
        AssignmentRow(label = "Envelope caps", value = "\u2212${budget.totalEnvelopeCaps.formatPhp()}", color = KapeBrown)
        AssignmentRow(label = "Active recurring expenses", value = "\u2212${budget.totalRecurringExpensesThisMonth.formatPhp()}", color = KapeBrown)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .background(KapeBrownSoft.copy(alpha = 0.2f))
        )

        AssignmentRow(
            label = if (budget.isOverAssigned) "Over-assigned by" else "Unassigned",
            value = (if (budget.isOverAssigned) (-budget.unassigned) else budget.unassigned).formatPhp(),
            color = if (budget.isOverAssigned) Terracotta else OceanTeal,
            bold = true
        )

        Text(
            text = "Goal contributions made this month aren't included above yet -- only " +
                "envelope caps and recurring expenses are counted as \"assigned\" right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun AssignmentRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = if (bold) KapeBrown else KapeBrownSoft
        )
        Text(
            text = value,
            style = (if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium).merge(TabularNumberStyle),
            color = color
        )
    }
}
