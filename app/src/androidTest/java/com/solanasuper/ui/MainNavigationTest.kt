package com.solanasuper.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solanasuper.network.MockArciumClient
import com.solanasuper.network.P2PTransferManager
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
        val mockPromptManager = Mockito.mock(BiometricPromptManager::class.java)
        val mockKeyManager = Mockito.mock(IdentityKeyManager::class.java)
        val mockArcium = Mockito.mock(MockArciumClient::class.java)
        // P2PTransferManager needs context, might be harder to mock if it's a concrete class in signature
        // We might need to pass a mock or null if allowed. 
        // For strict TDD, we assume the MainNavigation composable accepts these as params.
        
        composeTestRule.setContent {
            // MainNavigation doesn't exist yet, or hasn't been updated.
            // This references a Composable we EXPECT to implement.
             MainNavigation(
                 promptManager = mockPromptManager,
                 identityKeyManager = mockKeyManager,
                 arciumClient = mockArcium
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
