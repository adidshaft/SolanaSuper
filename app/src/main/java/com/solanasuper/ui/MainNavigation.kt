package com.solanasuper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solanasuper.data.TransactionDao
import com.solanasuper.network.MockArciumClient
import com.solanasuper.p2p.TransactionManager
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import com.solanasuper.network.NetworkManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.solanasuper.ui.governance.GovernanceScreen
import com.solanasuper.ui.health.HealthScreen
import com.solanasuper.ui.identity.IdentityHubScreen
import com.solanasuper.ui.income.IncomeScreen
import com.solanasuper.ui.income.IncomeViewModel

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
    arciumClient: MockArciumClient,
    transactionManager: TransactionManager,
    transactionDao: TransactionDao,
    p2pTransferManager: com.solanasuper.network.P2PTransferManager
) {
    val navController = rememberNavController()

    // Mesh Gradient Colors
    val deepBlack = Color(0xFF050505)
    val darkGray = Color(0xFF121212)
    val solanaGreen = Color(0xFF14F195)
    val solanaPurple = Color(0xFF9945FF)

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            deepBlack,
            darkGray,
            deepBlack,
            solanaPurple.copy(alpha = 0.15f),
            solanaGreen.copy(alpha = 0.1f),
            deepBlack
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    com.solanasuper.ui.components.PremiumBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent, // Allow gradient to show through
            bottomBar = {
                NavigationBar(
                    containerColor = deepBlack.copy(alpha = 0.8f) // Semi-transparent nav bar
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val items = listOf(Screen.Identity, Screen.Governance, Screen.Income, Screen.Health)
 
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                // Placeholder icon
                                Text(screen.label.first().toString(), color = Color.White) 
                            },
                            label = { Text(screen.label, color = Color.White) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = solanaGreen,
                                selectedTextColor = solanaGreen,
                                indicatorColor = solanaGreen.copy(alpha = 0.2f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
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
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Identity.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                composable(Screen.Identity.route) {
                    // Identity Hub (Pillar 2)
                    IdentityHubScreen(promptManager, identityKeyManager)
                }
                composable(Screen.Governance.route) {
                    // Governance (Pillar 3)
                    val viewModel: com.solanasuper.ui.governance.GovernanceViewModel = viewModel(
                        factory = com.solanasuper.ui.governance.GovernanceViewModel.Factory(promptManager, identityKeyManager, arciumClient)
                    )
                    
                    // Observe UI events
                    androidx.compose.runtime.LaunchedEffect(key1 = true) {
                        viewModel.uiEvent.collect { event ->
                            scope.launch {
                                snackbarHostState.showSnackbar(event)
                            }
                        }
                    }
                    
                    GovernanceScreen(viewModel)
                }
                composable(Screen.Income.route) {
                    // Income (Pillar 4)
                    val viewModel: IncomeViewModel = viewModel(
                        factory = IncomeViewModel.Factory(transactionManager, transactionDao, p2pTransferManager, identityKeyManager)
                    )
                    val state by viewModel.state.collectAsState()
                    
                    IncomeScreen(
                        state = state,
                        onClaimUbi = { viewModel.claimUbi() },
                        onSendOffline = { viewModel.startSending() },
                        onReceiveOffline = { viewModel.startReceiving() },
                        onCancelP2P = { viewModel.stopP2P() },
                        onConfirmP2P = { viewModel.confirmConnection() },
                        onRejectP2P = { viewModel.rejectConnection() }
                    )
                }
                composable(Screen.Health.route) {
                    // Health (Pillar 5)
                    val viewModel: com.solanasuper.ui.health.HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.solanasuper.ui.health.HealthViewModel.Factory(promptManager)
                    )
                    
                    // Observe UI events
                    androidx.compose.runtime.LaunchedEffect(key1 = true) {
                        viewModel.uiEvent.collect { event ->
                            scope.launch {
                                snackbarHostState.showSnackbar(event)
                            }
                        }
                    }
                    
                    val state by viewModel.state.collectAsState()
                    
                    HealthScreen(
                        state = state,
                        onUnlock = { viewModel.unlockVault() }
                    )
                }
            }
            
            // Network Mode Toggle (Overlay)
            val isLive by NetworkManager.isLiveMode.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, end = 16.dp), // Top Right
                contentAlignment = androidx.compose.ui.Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isLive) Color.Red.copy(alpha = 0.2f) else Color.Blue.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .clickable { NetworkManager.toggleMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isLive) "LIVE NET" else "SIMULATED",
                        color = if (isLive) Color.Red else Color.Cyan,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
}
