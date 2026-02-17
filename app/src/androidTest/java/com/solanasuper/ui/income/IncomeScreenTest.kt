package com.solanasuper.ui.income

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class IncomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun incomeScreen_displaysBalance_andActions() {
        // Mock ViewModel
        val viewModel = Mockito.mock(IncomeViewModel::class.java)
        val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(com.solanasuper.ui.income.IncomeUiState(balance = 100.0))
        Mockito.`when`(viewModel.state).thenReturn(stateFlow)
        Mockito.`when`(viewModel.signRequest).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        
        composeTestRule.setContent {
            IncomeScreen(viewModel = viewModel)
        }

        // 1. Verify Balance Card (Header)
        composeTestRule.onNodeWithText("Total Balance").assertIsDisplayed()

        // 2. Verify Action Buttons
        composeTestRule.onNodeWithText("Claim UBI (Faucet)").assertIsDisplayed()
        // "Offline Pay" was renamed/moved to "Send" modal or P2P section logic
        // Let's check for "Send" and "Receive" buttons
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
        composeTestRule.onNodeWithText("Receive").assertIsDisplayed()

        // 3. Verify Transaction List Header
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
    }

    @Test
    fun incomeScreen_showsP2PDialog_whenScanning() {
        // Arrange: State with P2P Scanning
        val scanningState = com.solanasuper.ui.income.IncomeUiState(
            p2pStatus = com.solanasuper.ui.income.PeerStatus.SCANNING
        )
        
        val viewModel = Mockito.mock(IncomeViewModel::class.java)
        val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(scanningState)
        Mockito.`when`(viewModel.state).thenReturn(stateFlow)
        Mockito.`when`(viewModel.signRequest).thenReturn(kotlinx.coroutines.flow.emptyFlow())

        composeTestRule.setContent {
            IncomeScreen(viewModel = viewModel)
        }

        // Assert: "Scanning for peers..." dialog/text is shown
        composeTestRule.onNodeWithText("Scanning for peers...").assertIsDisplayed()
    }
}
