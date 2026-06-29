package com.ipon.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.screens.addtransaction.AddTransactionScreen
import com.ipon.app.ui.screens.insights.InsightsScreen
import com.ipon.app.ui.screens.ledger.LedgerScreen
import com.ipon.app.ui.screens.plan.PlanScreen
import com.ipon.app.ui.screens.recap.YearRecapScreen
import com.ipon.app.ui.screens.recurring.AddRecurringTemplateScreen
import com.ipon.app.ui.screens.settings.LearnedCategoriesScreen
import com.ipon.app.ui.screens.settings.SettingsScreen
import com.ipon.app.ui.theme.OceanTeal

/**
 * Nav structure, v2: 4 bottom tabs (Ledger, Plan, Insights, Settings)
 * instead of the earlier 5 (Ledger, Envelopes, Recurring, Goals, Insights)
 * with Settings hidden behind a gear icon.
 *
 * Envelopes, Recurring, and Goals are now sub-tabs inside PlanScreen rather
 * than separate top-level destinations -- they're things you set up
 * occasionally and check on periodically, not things you visit as often as
 * the Ledger or Insights, so they don't need equal billing in the bottom
 * bar. Settings was promoted from a hidden gear icon to a real tab, since
 * it has grown real content (privacy statement, data management) that
 * deserves visibility rather than being tucked away.
 */
private sealed class IponDestination(val route: String, val label: String) {
    data object Ledger : IponDestination("ledger", "Ledger")
    data object Plan : IponDestination("plan", "Plan")
    data object Insights : IponDestination("insights", "Insights")
    data object Settings : IponDestination("settings", "Settings")

    /**
     * One route serves both "add new" and "edit existing" -- the
     * transactionId argument is nullable; AddTransactionScreen treats a
     * null arg as create-mode and a non-null one as edit-mode (see
     * AddTransactionViewModel.isEditing). Avoids duplicating the entire
     * form screen for what is otherwise identical UI.
     */
    data object AddTransaction : IponDestination("add_transaction?transactionId={transactionId}", "Add") {
        const val ARG_TRANSACTION_ID = "transactionId"
        fun createRoute(): String = "add_transaction"
        fun editRoute(transactionId: String): String = "add_transaction?transactionId=$transactionId"
    }

    data object AddRecurringTemplate : IponDestination("add_recurring_template", "Add recurring")
    data object LearnedCategories : IponDestination("learned_categories", "Learned categories")
    data object YearRecap : IponDestination("year_recap", "Year in Ipon")
}

@Composable
fun IponNavHost(viewModelFactory: IponViewModelFactory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Holds the ID of the most recently saved transaction so LedgerScreen can
    // play the spring-bounce entry animation (Section 5) on that one row when
    // the user returns from AddTransactionScreen. Cleared once consumed by
    // LedgerScreen's LaunchedEffect-driven appearance state, but since the row
    // itself only reads this once on first composition (see TransactionRow's
    // `remember(transaction.id) { mutableStateOf(!isNewlyAdded) }`), simply
    // leaving the value set here causes no harm if the user revisits the tab --
    // the row has already settled by then and won't replay the bounce.
    var recentlyAddedId by remember { mutableStateOf<String?>(null) }

    val bottomNavItems = listOf(
        IponDestination.Ledger,
        IponDestination.Plan,
        IponDestination.Insights,
        IponDestination.Settings
    )
    // Both "Add" screens are reached as modal-style pushes (the FAB, or the
    // "+ New recurring template" button inside Plan's Recurring sub-tab),
    // not via the bottom nav, so they're excluded from bottomNavItems but
    // still registered as NavHost routes. currentRoute reflects the
    // *resolved* route (with the real transactionId substituted in, or
    // omitted entirely for create-mode), so this checks the route's
    // destination ID prefix rather than an exact string match against the
    // template that contains the literal "{transactionId}" placeholder.
    val showBottomNav = currentRoute?.startsWith("add_transaction") != true &&
        currentRoute != IponDestination.AddRecurringTemplate.route &&
        currentRoute != IponDestination.LearnedCategories.route &&
        currentRoute != IponDestination.YearRecap.route

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {},
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OceanTeal,
                                selectedTextColor = OceanTeal
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Each screen below owns its own Scaffold/padding, so only the
        // bottom-nav-reserved space from this outer Scaffold is applied here
        // to avoid double-padding the content.
        NavHost(
            navController = navController,
            startDestination = IponDestination.Ledger.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(IponDestination.Ledger.route) {
                LedgerScreen(
                    viewModelFactory = viewModelFactory,
                    onAddTransactionClick = { navController.navigate(IponDestination.AddTransaction.createRoute()) },
                    onTransactionClick = { transactionId ->
                        navController.navigate(IponDestination.AddTransaction.editRoute(transactionId))
                    },
                    recentlyAddedId = recentlyAddedId
                )
            }
            composable(IponDestination.Plan.route) {
                PlanScreen(
                    viewModelFactory = viewModelFactory,
                    onAddRecurringTemplateClick = { navController.navigate(IponDestination.AddRecurringTemplate.route) }
                )
            }
            composable(IponDestination.AddRecurringTemplate.route) {
                AddRecurringTemplateScreen(
                    viewModelFactory = viewModelFactory,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(IponDestination.Insights.route) {
                InsightsScreen(
                    viewModelFactory = viewModelFactory,
                    onYearRecapClick = { navController.navigate(IponDestination.YearRecap.route) }
                )
            }
            composable(IponDestination.YearRecap.route) {
                YearRecapScreen(
                    viewModelFactory = viewModelFactory,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(IponDestination.Settings.route) {
                SettingsScreen(
                    viewModelFactory = viewModelFactory,
                    onLearnedCategoriesClick = { navController.navigate(IponDestination.LearnedCategories.route) }
                )
            }
            composable(IponDestination.LearnedCategories.route) {
                LearnedCategoriesScreen(
                    viewModelFactory = viewModelFactory,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                IponDestination.AddTransaction.route,
                arguments = listOf(
                    navArgument(IponDestination.AddTransaction.ARG_TRANSACTION_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                AddTransactionScreen(
                    viewModelFactory = viewModelFactory,
                    transactionIdToEdit = backStackEntry.arguments?.getString(IponDestination.AddTransaction.ARG_TRANSACTION_ID),
                    onSaved = { savedId ->
                        recentlyAddedId = savedId
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
