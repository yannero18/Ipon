package com.ipon.app.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.model.ExpenseCategory
import com.ipon.app.data.model.icon
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.icons.icon
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.TabularNumberStyle

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearRecapScreen(
    viewModelFactory: IponViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: RecapViewModel = viewModel(factory = viewModelFactory)
    val recap by viewModel.recap.collectAsState()

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("${recap.year} in Ipon", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = KapeBrownSoft) }
                }
            )
        }
    ) { padding ->
        if (recap.transactionCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing logged yet this year -- your recap will\nfill in as you go.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                HeroCard(
                    netSavedText = recap.netSaved.formatPhp(),
                    transactionCount = recap.transactionCount,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                StatRow(
                    label = "Total income",
                    value = recap.totalIncome.formatPhp(),
                    valueColor = OceanTeal,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StatRow(
                    label = "Total spent",
                    value = recap.totalExpense.formatPhp(),
                    valueColor = KapeBrown,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            if (recap.topCategories.isNotEmpty()) {
                item {
                    Text(
                        text = "WHERE IT WENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = KapeBrownSoft,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                items(recap.topCategories) { slice ->
                    val category = ExpenseCategory.fromDisplayName(slice.category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = category.icon(),
                                contentDescription = null,
                                tint = KapeBrown,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = slice.category,
                                style = MaterialTheme.typography.bodyLarge,
                                color = KapeBrown,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            text = slice.total.formatPhp(),
                            style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
                            color = KapeBrown
                        )
                    }
                }
                item { Spacer(modifier = Modifier.padding(top = 6.dp)) }
            }

            recap.busiestMonth?.let { busiest ->
                item {
                    InfoCard(
                        title = MONTH_NAMES.getOrElse(busiest.monthIndex) { "This month" },
                        subtitle = "Your busiest month -- ${busiest.transactionCount} entries logged",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            if (recap.totalReflections > 0) {
                item {
                    MoodSummaryCard(
                        moodCounts = recap.moodCounts,
                        totalReflections = recap.totalReflections,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            if (recap.goalsCompletedThisYear > 0 || !recap.totalSavedTowardGoals.isZero) {
                item {
                    InfoCard(
                        title = "${recap.totalSavedTowardGoals.formatPhp()} saved toward goals",
                        subtitle = if (recap.goalsCompletedThisYear > 0) {
                            "${recap.goalsCompletedThisYear} goal${if (recap.goalsCompletedThisYear == 1) "" else "s"} reached this year"
                        } else {
                            "Keep going"
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun HeroCard(
    netSavedText: String,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(OceanTeal)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "NET THIS YEAR",
                style = MaterialTheme.typography.labelSmall,
                color = RicePaper.copy(alpha = 0.7f)
            )
            Text(
                text = netSavedText,
                style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
                color = RicePaper,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )
            Text(
                text = "from $transactionCount logged ${if (transactionCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.bodyMedium,
                color = RicePaper.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = KapeBrownSoft)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.merge(TabularNumberStyle),
            color = valueColor
        )
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = KapeBrown)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = KapeBrownSoft,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun MoodSummaryCard(
    moodCounts: Map<MoodRating, Int>,
    totalReflections: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "$totalReflections check-in${if (totalReflections == 1) "" else "s"} this year",
            style = MaterialTheme.typography.bodyLarge,
            color = KapeBrown,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            MoodRating.entries.forEach { mood ->
                val count = moodCounts[mood] ?: 0
                if (count > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = mood.icon(),
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(28.dp).padding(bottom = 4.dp)
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                            color = KapeBrownSoft
                        )
                    }
                }
            }
        }
    }
}
