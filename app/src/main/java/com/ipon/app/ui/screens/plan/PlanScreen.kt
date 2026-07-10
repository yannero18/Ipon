package com.ipon.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.screens.budgetplan.BudgetPlanScreen
import com.ipon.app.ui.screens.calendar.CalendarScreen
import com.ipon.app.ui.screens.debts.DebtsScreen
import com.ipon.app.ui.screens.envelopes.EnvelopesScreen
import com.ipon.app.ui.screens.goals.GoalsScreen
import com.ipon.app.ui.screens.recurring.RecurringScreen
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.HairlineBorder

private enum class PlanTab(val label: String) {
    ENVELOPES("Envelopes"),
    RECURRING("Recurring"),
    GOALS("Goals"),
    DEBTS("Debts"),
    BUDGET("Budget"),
    CALENDAR("Calendar")
}

val OrganicUnderlineShape = RoundedCornerShape(
    topStart = 6.dp,
    topEnd = 2.dp,   
    bottomEnd = 6.dp,
    bottomStart = 2.dp
)

@Composable
fun LookbookTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent, // Kills the grey container
        edgePadding = 16.dp, // Generous padding to prevent awkward edge wrapping
        divider = {}, // Kills the standard rigid bottom border
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                // This is your custom "squiggle" active indicator
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .padding(horizontal = 12.dp) // Keeps the underline shorter than the word
                        .height(4.dp) // A heavy, deliberate ink stroke
                        .background(
                            color = OceanTeal,
                            shape = OrganicUnderlineShape // Applies the asymmetric look
                        )
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        // High contrast: Teal if active, muted Kape Brown if inactive
                        color = if (selectedTabIndex == index) OceanTeal else KapeBrown.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1 // Strict constraint to prevent awkward wrapping
                    )
                }
            )
        }
    }
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
    onAddRecurringTemplateClick: () -> Unit,
    onCreateGoalClick: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(containerColor = RicePaper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LOOKBOOK PLANNER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = KapeBrownSoft
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Plan Budgets",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrown
                    )
                }

                // Lookbook aesthetic option slot on the top-right corner
                IconButton(
                    onClick = { /* Action placeholder */ },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, HairlineBorder, CircleShape)
                ) {
                    Text(
                        text = "•••",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = KapeBrown,
                        modifier = Modifier.padding(bottom = 4.dp) // Align vertically
                    )
                }
            }

            LookbookTabRow(
                selectedTabIndex = selectedTab,
                tabs = PlanTab.entries.map { it.label },
                onTabSelected = { selectedTab = it }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (PlanTab.entries[selectedTab]) {
                    PlanTab.ENVELOPES -> EnvelopesScreen(viewModelFactory = viewModelFactory)
                    PlanTab.RECURRING -> RecurringScreen(
                        viewModelFactory = viewModelFactory,
                        onAddTemplateClick = onAddRecurringTemplateClick
                    )
                    PlanTab.GOALS -> GoalsScreen(
                        viewModelFactory = viewModelFactory,
                        onCreateGoalClick = onCreateGoalClick,
                        onGoalClick = onGoalClick
                    )
                    PlanTab.DEBTS -> DebtsScreen(viewModelFactory = viewModelFactory)
                    PlanTab.BUDGET -> BudgetPlanScreen(viewModelFactory = viewModelFactory)
                    PlanTab.CALENDAR -> CalendarScreen(viewModelFactory = viewModelFactory)
                }
            }
        }
    }
}
