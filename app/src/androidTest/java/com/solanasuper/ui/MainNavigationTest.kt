package com.solanasuper.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solanasuper.network.MockArciumClient
import com.solanasuper.p2p.TransactionManager
import com.solanasuper.data.TransactionDao
import com.solanasuper.security.BiometricPromptManager
import com.solanasuper.security.IdentityKeyManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainNavigation_verifiesBottomBarClick_andScreenSwitching() {
        // Mock dependencies
        val promptManager = Mockito.mock(BiometricPromptManager::class.java)
        val identityKeyManager = Mockito.mock(IdentityKeyManager::class.java)
        val arciumClient = Mockito.mock(MockArciumClient::class.java)
        
        val transactionDao = Mockito.mock(TransactionDao::class.java)
        val transactionManager = Mockito.mock(TransactionManager::class.java)
        val p2pTransferManager = Mockito.mock(com.solanasuper.network.P2PTransferManager::class.java)
        
        // Mock Repositories
        val activityRepository = Mockito.mock(com.solanasuper.data.ActivityRepository::class.java)
        val healthRepository = Mockito.mock(com.solanasuper.data.HealthRepository::class.java)
        
        composeTestRule.setContent {
             MainNavigation(
                 promptManager = promptManager,
                 identityKeyManager = identityKeyManager,
                 arciumClient = arciumClient,
                 transactionManager = transactionManager,
                 transactionDao = transactionDao,
                 p2pTransferManager = p2pTransferManager,
                 activityRepository = activityRepository,
                 healthRepository = healthRepository
             )
        }

        // 1. Verify Start Screen (Governance) - Changed from Identity Hub to Governance as start destination
        composeTestRule.onNodeWithText("Democracy Dashboard").assertIsDisplayed()

        // 2. Navigate to Validation (Identity/Hub is hidden from tab bar but accessible? No, Profile is new tab)
        // Check tabs availability
        // Tabs: G, I, H, P
        
        // Navigate to Income
        composeTestRule.onNodeWithContentDescription("Income").performClick()
        composeTestRule.onNodeWithText("Income & Assets").assertIsDisplayed() // Updated Title

        // Navigate to Health
        composeTestRule.onNodeWithContentDescription("Health").performClick()
        // Might be "Health Vault" or "Secure Health Vault" depending on Locked state UI
        composeTestRule.onNodeWithText("Health Vault").assertIsDisplayed()
    }
}
