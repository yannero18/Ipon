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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.screens.addtransaction.AddTransactionScreen
import com.ipon.app.ui.screens.insights.InsightsScreen
import com.ipon.app.ui.screens.ledger.LedgerScreen
import com.ipon.app.ui.theme.OceanTeal

private sealed class IponDestination(val route: String, val label: String) {
    data object Ledger : IponDestination("ledger", "Ledger")
    data object Insights : IponDestination("insights", "Insights")
    data object AddTransaction : IponDestination("add_transaction", "Add")
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

    val bottomNavItems = listOf(IponDestination.Ledger, IponDestination.Insights)
    // The Add Transaction screen is reached via the coin FAB, not the bottom
    // nav, so it is excluded from bottomNavItems but still a NavHost route.
    val showBottomNav = currentRoute != IponDestination.AddTransaction.route

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
                    onAddTransactionClick = { navController.navigate(IponDestination.AddTransaction.route) },
                    recentlyAddedId = recentlyAddedId
                )
            }
            composable(IponDestination.Insights.route) {
                InsightsScreen(viewModelFactory = viewModelFactory)
            }
            composable(IponDestination.AddTransaction.route) {
                AddTransactionScreen(
                    viewModelFactory = viewModelFactory,
                    onSaved = { savedId ->
                        recentlyAddedId = savedId
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
