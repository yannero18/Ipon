package com.ipon.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.CategoryTrend
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.SavingsSuggestion
import com.ipon.app.data.model.SuggestionSeverity
import com.ipon.app.data.model.TrendDirection
import com.ipon.app.data.model.CategorySlice
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import com.ipon.app.ui.theme.Terracotta
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun InsightsScreen(
    viewModelFactory: IponViewModelFactory,
    onYearRecapClick: () -> Unit
) {
    val viewModel: InsightsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = RicePaper) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "This month's breakdown",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Total spent: ${uiState.summary.expense.formatPhp()}",
                            style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                            color = KapeBrownSoft
                        )
                    }
                    TextButton(onClick = onYearRecapClick) {
                        Text("Year in Ipon \u2192", color = OceanTeal)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (uiState.suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "WORTH A LOOK",
                        style = MaterialTheme.typography.labelSmall,
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
                        style = MaterialTheme.typography.labelSmall,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                items(uiState.trends.take(3), key = { it.category }) { trend ->
                    TrendRow(trend = trend, modifier = Modifier.padding(bottom = 10.dp))
                }
                item { Spacer(modifier = Modifier.height(14.dp)) }
            }

            if (uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
                item {
                    Text(
                        text = "No expenses logged yet this month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                }
            }

            items(uiState.categoryBreakdown) { slice ->
                CategoryBreakdownRow(
                    slice = slice,
                    totalExpenseMinorUnits = uiState.summary.expense.minorUnits,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
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
            .background(Color.White)
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
        TrendDirection.DOWN -> OceanTeal
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
    // Safe to resolve as ExpenseCategory unconditionally: this screen's
    // underlying DAO query (observeCategoryBreakdownBetween) filters
    // WHERE type = 'EXPENSE', so slice.category never holds an income label.
    val category = ExpenseCategory.fromDisplayName(slice.category)

    // Percentage is purely a display concern, so this is the one place a
    // BigDecimal ratio is computed from two Money values -- it never feeds
    // back into stored amounts, keeping the ledger's core math integer-only.
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
                style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
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
                    .background(OceanTeal)
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
