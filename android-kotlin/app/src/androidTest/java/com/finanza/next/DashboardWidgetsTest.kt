package com.finanza.next

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Before
import org.junit.Test

class DashboardWidgetsTest {
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
    fun editorExposesWebDashboardControls() {
        composeRule.onNodeWithText("Editar").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Editar widgets").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Editar widgets").fetchSemanticsNode()
        check(composeRule.onAllNodesWithText("Atalhos do Finanza").fetchSemanticsNodes().size >= 2)
        composeRule.onNodeWithText("Essencial").fetchSemanticsNode()
        composeRule.onNodeWithText("Todos").fetchSemanticsNode()
        composeRule.onNodeWithText("Concluir").performScrollTo().assertIsDisplayed()
    }
}
