package com.solanasuper.ui.income

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun incomeScreen_displaysBalance_andActions() {
        composeTestRule.setContent {
            // We expect the IncomeScreen to eventually take a state or ViewModel
            // For now, we test the current placeholder or the future contract.
            // Since we follow TDD, we run against what we have or what we will build.
            // Let's assume we will pass a static state for testing or just test the composable.
            // Currently IncomeScreen() takes no args. 
            // We will refactor it later.
            IncomeScreen()
        }

        // 1. Verify Balance Card (Header)
        // We expect to see "Total Balance" or similar
        composeTestRule.onNodeWithText("Total Balance").assertIsDisplayed()

        // 2. Verify Action Buttons
        composeTestRule.onNodeWithText("Claim UBI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline Pay").assertIsDisplayed()

        // 3. Verify Transaction List Header
        composeTestRule.onNodeWithText("Recent Transactions").assertIsDisplayed()
    }

    @Test
    fun incomeScreen_showsP2PDialog_whenScanning() {
        // Arrange: State with P2P Scanning
        val scanningState = IncomeState(
            p2pStatus = P2PStatus.SCANNING
        )

        composeTestRule.setContent {
            IncomeScreen(state = scanningState)
        }

        // Assert: "Scanning for peers..." dialog/text is shown
        composeTestRule.onNodeWithText("Scanning for peers...").assertIsDisplayed()
    }
}
