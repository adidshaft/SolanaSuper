package com.solanasuper.ui.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun healthScreen_startsLocked_andHidesRecords() {
        composeTestRule.setContent {
            // Placeholder: In Green phase we will inject state/ViewModel
            HealthScreen()
        }

        // 1. Verify Locked UI
        // Expect a title or button indicating it's locked
        composeTestRule.onNodeWithText("Secure Health Vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap to Unlock").assertIsDisplayed()

        // 2. Verify Data is HIDDEN
        // "Vaccine Certificate" is the mock data we expect AFTER unlock
        // So initially it should NOT exist
        composeTestRule.onNodeWithText("Vaccine Certificate").assertDoesNotExist()
    }
}
