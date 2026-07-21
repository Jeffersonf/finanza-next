package com.finanza.next

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finanza.next.ui.screens.QuickExpenseFlow
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickExpenseFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureMovesDirectlyFromValueToDescription() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true) {
                QuickExpenseFlow(
                    methods = emptyList(),
                    onComplete = { _, _, _ -> },
                    onDismiss = {}
                )
            }
        }
        composeRule.onNodeWithText("Qual o valor do gasto?").assertExists()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("1234")
        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("O que foi?").assertExists()
        composeRule.onNodeWithText("Qual a categoria?").assertDoesNotExist()
        composeRule.onNodeWithText("Como você pagou?").assertDoesNotExist()
    }

    @Test
    fun captureRendersInsideFinanzaExperience() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.FINANZA) {
                QuickExpenseFlow(
                    methods = emptyList(),
                    onComplete = { _, _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Qual o valor do gasto?").assertExists()
        composeRule.onNodeWithText("R$ 0,00").assertExists()
    }
}
