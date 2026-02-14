package com.solanasuper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solanasuper.network.MockArciumClient
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.ui.governance.GovernanceScreen
import com.solanasuper.ui.health.HealthScreen
import com.solanasuper.ui.identity.IdentityHubScreen
import com.solanasuper.ui.income.IncomeScreen

// Define generic icon resource or use vector assets if available.
// For now, we reuse standard icons or placeholder IDs if resources aren't checked.
// Assuming we can use simple Text or default icons if resources missing.
// But to be safe, let's use Text/Icon with standard material icons if possible,
// OR just placeholder icons (0) if we don't have R.drawable yet.
// Actually, standard Material icons are in `androidx.compose.material.icons` but we may need dependency.
// Let's use simple Text labels for tabs to pass the test first.

sealed class Screen(val route: String, val label: String) {
    object Identity : Screen("identity", "Identity")
    object Governance : Screen("governance", "Governance")
    object Income : Screen("income", "Income")
    object Health : Screen("health", "Health")
}

@Composable
fun MainNavigation(
    promptManager: BiometricPromptManager,
    identityKeyManager: IdentityKeyManager,
    arciumClient: MockArciumClient
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(Screen.Identity, Screen.Governance, Screen.Income, Screen.Health)

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            // Placeholder icon
                            Text(screen.label.first().toString()) 
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Identity.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Identity.route) {
                // Identity Hub (Pillar 2)
                IdentityHubScreen(promptManager, identityKeyManager)
            }
            composable(Screen.Governance.route) {
                // Governance (Pillar 3)
                GovernanceScreen(promptManager, identityKeyManager, arciumClient)
            }
            composable(Screen.Income.route) {
                // Income (Pillar 4)
                IncomeScreen()
            }
            composable(Screen.Health.route) {
                // Health (Pillar 5)
                HealthScreen()
            }
        }
    }
}
