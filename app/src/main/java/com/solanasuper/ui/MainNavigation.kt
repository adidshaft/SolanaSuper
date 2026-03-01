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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import com.solanasuper.ui.governance.GovernanceScreen
import com.solanasuper.ui.health.HealthScreen
import com.solanasuper.ui.identity.IdentityHubScreen
import com.solanasuper.ui.income.IncomeScreen
import com.solanasuper.ui.income.IncomeViewModel
import com.solanasuper.ui.invest.InvestScreen
import com.solanasuper.ui.invest.InvestViewModel

// Define generic icon resource or use vector assets if available.
// For now, we reuse standard icons or placeholder IDs if resources aren't checked.
// Assuming we can use simple Text or default icons if resources missing.
// But to be safe, let's use Text/Icon with standard material icons if possible,
// OR just placeholder icons (0) if we don't have R.drawable yet.
// Actually, standard Material icons are in `androidx.compose.material.icons` but we may need dependency.
// Let's use simple Text labels for tabs to pass the test first.

sealed class Screen(val route: String, val label: String) {
    object Welcome : Screen("welcome", "Welcome")
    object Identity : Screen("identity", "Identity")
    object Governance : Screen("governance", "Gov")
    object Income : Screen("income", "Wallet")
    object Invest : Screen("invest", "Invest")
    object Health : Screen("health", "Health")
    object Profile : Screen("profile", "Me")
}

@Composable
fun MainNavigation(
    promptManager: BiometricPromptManager,
    identityKeyManager: IdentityKeyManager,
    arciumClient: MockArciumClient,
    transactionManager: TransactionManager,
    transactionDao: TransactionDao,
    p2pTransferManager: com.solanasuper.network.P2PTransferManager,
    activityRepository: com.solanasuper.data.ActivityRepository,
    healthRepository: com.solanasuper.data.HealthRepository,
    investDao: com.solanasuper.data.InvestDao,
    nonceAccountDao: com.solanasuper.data.NonceAccountDao
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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                if (currentRoute != Screen.Welcome.route) {
                    NavigationBar(
                        containerColor = deepBlack.copy(alpha = 0.8f) // Semi-transparent nav bar
                    ) {
                        val currentDestination = navBackStackEntry?.destination
                        // Modified: Removed Identity from Tab, replaced with Profile as the "Identity" tab technically
                    // Or keep Identity (Hub) and add Profile? 
                    // Let's add Profile as the 5th tab or replace one?
                    // "Create a new ProfileScreen.kt... As a user I need to view my solana address"
                    // IdentityHub was more about "Biometrics". Profile is "Wallet + History".
                    // Let's keep them all for now or user might complain features are missing.
                    // But 5 tabs is crowded.
                    // Let's swap "Identity" (Hub) with "Profile" for the bottom bar, 
                    // and maybe IdentityHub is accessible inside Profile?
                    // OR just append Profile.
                    
                    val items = listOf(Screen.Governance, Screen.Income, Screen.Invest, Screen.Health, Screen.Profile)

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Governance -> Icons.Default.Star
                                        Screen.Income     -> Icons.Default.Send
                                        Screen.Invest     -> Icons.Default.Add
                                        Screen.Health     -> Icons.Default.Favorite
                                        Screen.Profile    -> Icons.Default.Person
                                        else              -> Icons.Default.Person
                                    },
                                    contentDescription = screen.label
                                )
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
        }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Welcome.route, // Changed start to Welcome splash screen
                    modifier = Modifier.fillMaxSize()
                ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(onTimeout = {
                        navController.navigate(Screen.Governance.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Identity.route) {
                    // Identity Hub (Pillar 2) - Kept available if routed, but hidden from main tabs
                    IdentityHubScreen(promptManager, identityKeyManager)
                }
                composable(Screen.Governance.route) {
                    // Governance (Pillar 3)
                    val viewModel: com.solanasuper.ui.governance.GovernanceViewModel = viewModel(
                        factory = com.solanasuper.ui.governance.GovernanceViewModel.Factory(
                            promptManager, 
                            identityKeyManager, 
                            arciumClient,
                            activityRepository // Inject Repo
                        )
                    )
                    
                    // Observe UI events
                    androidx.compose.runtime.LaunchedEffect(key1 = true) {
                        viewModel.uiEvent.collect { event ->
                            scope.launch {
                                snackbarHostState.showSnackbar(event)
                            }
                        }
                    }
                    
                    // Observe Sign Requests
                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.runtime.LaunchedEffect(key1 = true) {
                        viewModel.signRequest.collect { (choice, payload) ->
                            try {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    val signature = identityKeyManager.signTransaction(activity, payload)
                                    viewModel.onVoteSigned(signature)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error: Context is not FragmentActivity") }
                                }
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Signing Failed: ${e.message}") }
                            }
                        }
                    }
                    
                    GovernanceScreen(viewModel)
                }
                composable(Screen.Income.route) {
                    // Income (Pillar 4)
                    val viewModel: IncomeViewModel = viewModel(
                        factory = IncomeViewModel.Factory(transactionManager, transactionDao, p2pTransferManager, identityKeyManager, nonceAccountDao)
                    )
                    
                    // Observe UI Events (Added for Faucet Fallback)
                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.runtime.LaunchedEffect(key1 = true) {
                        viewModel.uiEvent.collect { event ->
                            if (event.startsWith("OPEN_URL|")) {
                                val url = event.removePrefix("OPEN_URL|")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    }
                    
                    // State collection happens internally in IncomeScreen now for simplicity of this refactor
                    IncomeScreen(viewModel = viewModel)
                }
                composable(Screen.Invest.route) {
                    val viewModel: InvestViewModel = viewModel(
                        factory = InvestViewModel.Factory(identityKeyManager, investDao, transactionDao)
                    )
                    InvestScreen(viewModel = viewModel)
                }
                composable(Screen.Health.route) {
                    // Health (Pillar 5)
                    val viewModel: com.solanasuper.ui.health.HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.solanasuper.ui.health.HealthViewModel.Factory(
                            promptManager,
                            activityRepository, // Inject Repo
                            healthRepository
                        )
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
                        viewModel = viewModel
                    )
                }
                composable(Screen.Profile.route) {
                    // Profile (Phase 9)
                    val viewModel: com.solanasuper.ui.profile.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.solanasuper.ui.profile.ProfileViewModel.Factory(identityKeyManager, activityRepository)
                    )
                    
                    com.solanasuper.ui.profile.ProfileScreen(
                        viewModel = viewModel,
                        onShowSnackbar = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }
            
            // Network Mode Toggle (Sleek Minimalist Capsule)
            val isLive by NetworkManager.isLiveMode.collectAsState()
            
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            if (currentRoute != Screen.Welcome.route) {
                Box(
                    modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 24.dp), 
                contentAlignment = androidx.compose.ui.Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { NetworkManager.toggleMode() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        // Status Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isLive) Color(0xFF14F195) else Color(0xFF00C2FF),
                                    shape = CircleShape
                                )
                        )
                        
                        Text(
                            text = if (isLive) "LIVE" else "SIMULATION",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
    }
    }
}
