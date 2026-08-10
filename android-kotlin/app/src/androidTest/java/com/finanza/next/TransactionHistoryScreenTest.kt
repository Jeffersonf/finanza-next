package com.finanza.next

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.screens.TransactionHistoryScreen
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Before

class TransactionHistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun freezeTestClock() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun historyGroupsUsingTheIsoDateAndFiltersByTransactionType() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.NEXT) {
                TransactionHistoryScreen(
                    transactions = listOf(
                        TransactionUi(1, "Mercado", "Alimentacao", "-R$ 80,00", "2 ago", false, Color.Red, dateKey = "2026-08-02"),
                        TransactionUi(2, "Salario", "Renda", "+R$ 1.000,00", "15 jul", true, Color.Green, dateKey = "2026-07-15")
                    ),
                    onTransaction = {},
                    onQuickAdd = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("08/2026").assertIsDisplayed()
        composeRule.onNodeWithText("07/2026").assertIsDisplayed()
        composeRule.onNodeWithText("Gastos").performClick()
        composeRule.onNodeWithText("Mercado").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Salario").fetchSemanticsNodes().isEmpty())
    }
}
