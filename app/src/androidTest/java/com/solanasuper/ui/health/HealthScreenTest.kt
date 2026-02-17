package com.solanasuper.ui.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class HealthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun healthScreen_startsLocked_andHidesRecords() {
        // Mock ViewModel
        val viewModel = Mockito.mock(HealthViewModel::class.java)
        val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(com.solanasuper.ui.health.HealthState(isLocked = true))
        Mockito.`when`(viewModel.state).thenReturn(stateFlow)
        
        // Mock UI Event flow
        Mockito.`when`(viewModel.uiEvent).thenReturn(kotlinx.coroutines.flow.emptyFlow())

        composeTestRule.setContent {
            HealthScreen(viewModel = viewModel)
        }

        // 1. Verify Locked UI
        composeTestRule.onNodeWithText("Health Vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap icon to unlock").assertIsDisplayed() // Updated Text

        // 2. Verify Data is HIDDEN
        composeTestRule.onNodeWithText("Vaccine Certificate").assertDoesNotExist()
    }
}
