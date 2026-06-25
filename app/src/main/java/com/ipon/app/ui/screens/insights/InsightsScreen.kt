package com.ipon.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.model.Category
import com.ipon.app.data.repository.CategorySlice
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.TabularNumberStyle
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun InsightsScreen(viewModelFactory: IponViewModelFactory) {
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
                Text(
                    text = "This month's breakdown",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Total spent: ${uiState.summary.expense.formatPhp()}",
                    style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
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
private fun CategoryBreakdownRow(
    slice: CategorySlice,
    totalExpenseMinorUnits: Long,
    modifier: Modifier = Modifier
) {
    val category = Category.fromDisplayName(slice.category)

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
            Text(text = "${category.emoji} ${slice.category}", style = MaterialTheme.typography.bodyLarge)
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
