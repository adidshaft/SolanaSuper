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
        // P2PTransferManager needs context, might be harder to mock if it's a concrete class in signature
        // We might need to pass a mock or null if allowed. 
        // For strict TDD, we assume the MainNavigation composable accepts these as params.
        
        // Mock new dependencies
        val transactionDao = Mockito.mock(TransactionDao::class.java)
        val transactionManager = Mockito.mock(TransactionManager::class.java)
        // Mock P2PTransferManager (needs to be mocked or real context provided)
        val p2pTransferManager = Mockito.mock(com.solanasuper.network.P2PTransferManager::class.java)
        
        composeTestRule.setContent {
            // MainNavigation updated signature
             MainNavigation(
                 promptManager = promptManager,
                 identityKeyManager = identityKeyManager,
                 arciumClient = arciumClient,
                 transactionManager = transactionManager,
                 transactionDao = transactionDao,
                 p2pTransferManager = p2pTransferManager
             )
        }

        // 1. Verify Start Screen (Identity)
        composeTestRule.onNodeWithText("Identity Hub").assertIsDisplayed()

        // 2. Navigate to Governance
        composeTestRule.onNodeWithContentDescription("Governance").performClick()
        composeTestRule.onNodeWithText("Democracy Dashboard").assertIsDisplayed()

        // 3. Navigate to Income
        composeTestRule.onNodeWithContentDescription("Income").performClick()
        composeTestRule.onNodeWithText("Sovereign Wallet").assertIsDisplayed()

        // 4. Navigate to Health
        composeTestRule.onNodeWithContentDescription("Health").performClick()
        composeTestRule.onNodeWithText("Secure Health Vault").assertIsDisplayed()
    }
}
