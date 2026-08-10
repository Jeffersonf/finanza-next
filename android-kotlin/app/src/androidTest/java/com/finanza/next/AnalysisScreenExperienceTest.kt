package com.finanza.next

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.screens.AnalysisScreen
import com.finanza.next.ui.screens.CategoryUi
import com.finanza.next.ui.screens.MonthTrendUi
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Before

class AnalysisScreenExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun freezeTestClock() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun modernKeepsExtendedFiltersCollapsedUntilRequested() {
        render(AppExperience.NEXT)

        composeRule.onNodeWithText("Mais filtros").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Valor mínimo").assertIsDisplayed()
        composeRule.onNodeWithText("Valor máximo").assertIsDisplayed()
    }

    @Test
    fun classicDoesNotReceiveModernFilterToggle() {
        render(AppExperience.FINANZA)

        assertTrue(composeRule.onAllNodesWithText("Mais filtros").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun finanzaWebKeepsAmountFiltersDirectlyAvailable() {
        render(AppExperience.WEB)

        composeRule.onNodeWithText("Valor mínimo").assertIsDisplayed()
        composeRule.onNodeWithText("Valor máximo").assertIsDisplayed()
    }

    private fun render(experience: AppExperience) {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = experience) {
                AnalysisScreen(
                    income = "R$ 1.000,00",
                    spent = "R$ 320,00",
                    categories = listOf(CategoryUi("Alimentação", "R$ 320,00", 1f, Color(0xFFE05D5D))),
                    trends = listOf(MonthTrendUi("Jul", "R$ 1.000,00", "R$ 320,00", 1f, 0.32f)),
                    transactions = listOf(
                        TransactionUi(
                            id = 1,
                            title = "Mercado",
                            category = "Alimentação",
                            amount = "R$ 320,00",
                            date = "2026-07-22",
                            income = false,
                            color = Color(0xFFE05D5D),
                            accountId = "main"
                        )
                    ),
                    accounts = listOf(AccountUi("main", "Principal", "conta corrente", "R$ 680,00", "wallet")),
                    onTransaction = {}
                )
            }
        }
    }
}
