package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Before
import org.junit.Test

class NavigationGesturesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun enterLocalModeWhenLoginIsShown() {
        val offline = composeRule.onAllNodesWithText("Usar somente neste aparelho")
        if (offline.fetchSemanticsNodes().isNotEmpty()) {
            offline[0].performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun swipingLeftMovesToNextScreen() {
        composeRule.onNodeWithText("Atalhos do Finanza").assertIsDisplayed()
        composeRule.onNodeWithTag("mainPager").performTouchInput { swipeLeft(durationMillis = 1_000) }
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithTag("accountsScreen").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithTag("accountsScreen").assertIsDisplayed()
    }

    @Test
    fun tappingBottomNavigationChangesScreenWithoutLeavingTheScaffold() {
        composeRule.onNodeWithText("Atalhos do Finanza").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Contas").performClick()

        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithTag("accountsScreen").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithTag("accountsScreen").assertIsDisplayed()
    }
}
