package com.ipon.app.ui.screens.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.screens.budgetplan.BudgetPlanScreen
import com.ipon.app.ui.screens.debts.DebtsScreen
import com.ipon.app.ui.screens.envelopes.EnvelopesScreen
import com.ipon.app.ui.screens.goals.GoalsScreen
import com.ipon.app.ui.screens.recurring.RecurringScreen
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper

private enum class PlanTab(val label: String) {
    ENVELOPES("Envelopes"),
    RECURRING("Recurring"),
    GOALS("Goals"),
    DEBTS("Debts"),
    BUDGET("Budget")
}

/**
 * Section housing Envelopes, Recurring, and Goals as sub-tabs within one
 * bottom-nav destination, rather than three separate top-level tabs.
 *
 * Why: these three are things you set up occasionally and check on
 * periodically, not things checked as often as the Ledger or Insights --
 * grouping them under one "Plan" destination keeps the bottom nav at 4
 * tabs (Ledger, Plan, Insights, Settings) instead of letting every new
 * planning feature add another permanent slot to the bottom bar.
 */
@Composable
fun PlanScreen(
    viewModelFactory: IponViewModelFactory,
    onAddRecurringTemplateClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(containerColor = RicePaper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = RicePaper,
                contentColor = OceanTeal
            ) {
                PlanTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label, style = MaterialTheme.typography.bodyMedium) }
                    )
                }
            }

            when (PlanTab.entries[selectedTab]) {
                PlanTab.ENVELOPES -> EnvelopesScreen(viewModelFactory = viewModelFactory)
                PlanTab.RECURRING -> RecurringScreen(
                    viewModelFactory = viewModelFactory,
                    onAddTemplateClick = onAddRecurringTemplateClick
                )
                PlanTab.GOALS -> GoalsScreen(viewModelFactory = viewModelFactory)
                PlanTab.DEBTS -> DebtsScreen(viewModelFactory = viewModelFactory)
                PlanTab.BUDGET -> BudgetPlanScreen(viewModelFactory = viewModelFactory)
            }
        }
    }
}
