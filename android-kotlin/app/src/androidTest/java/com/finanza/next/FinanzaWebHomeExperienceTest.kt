package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.graphics.Color
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.BillStatus
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.screens.CategoryUi
import com.finanza.next.ui.screens.HomeScreen
import com.finanza.next.ui.screens.MonthTrendUi
import com.finanza.next.ui.screens.TransactionHistoryScreen
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test

class FinanzaWebHomeExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun webHomeKeepsTheSiteShellAndCompactSummary() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.WEB) {
                HomeScreen(
                    userName = "Jefferson",
                    greeting = "Olá",
                    period = "Ago",
                    balance = "R$ 2.256,00",
                    income = "R$ 0,00",
                    monthlyIncome = "R$ 0,00",
                    spent = "R$ 2.744,00",
                    transactions = listOf(
                        TransactionUi(1, "Mercado", "Alimentação", "R$ 20,00", "2026-08-01", false, Color.Red, "main")
                    ),
                    accounts = listOf(AccountUi("main", "Principal", "conta", "R$ 2.256,00", "wallet")),
                    bills = listOf(BillUi(2, "Aluguel", "Conta", "2026-08-10", "R$ 900,00", BillStatus.PENDENTE)),
                    categories = listOf(CategoryUi("Alimentação", "R$ 20,00", 1f, Color.Red)),
                    trends = listOf(MonthTrendUi("Ago", "R$ 0,00", "R$ 20,00", 0f, 1f)),
                    features = FeatureCenterUiState(emptyList(), pendingSync = 0, online = true),
                    onAdd = {},
                    onAll = {},
                    onTransaction = {},
                    onBill = {},
                    onFeatures = {},
                    onImport = {},
                    onAccounts = {},
                    onAnalysis = {},
                    onSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("Início").assertIsDisplayed()
        composeRule.onNodeWithText("Saldo disponível").assertIsDisplayed()
        composeRule.onNodeWithText("Entradas").assertIsDisplayed()
        composeRule.onNodeWithText("Saídas").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 2.256,00").assertIsDisplayed()
    }

    @Test
    fun webHistorySupportsAmountRangeFiltering() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.WEB) {
                TransactionHistoryScreen(
                    transactions = listOf(
                        TransactionUi(1, "Mercado", "Alimentação", "R$ 20,00", "2026-08-01", false, Color.Red, "main"),
                        TransactionUi(2, "Aluguel", "Casa", "R$ 900,00", "2026-08-02", false, Color.Blue, "main")
                    ),
                    accounts = listOf(AccountUi("main", "Principal", "conta", "R$ 2.256,00", "wallet")),
                    onTransaction = {},
                    onQuickAdd = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Valor mínimo").performTextInput("100")
        composeRule.onNodeWithText("Aluguel").assertIsDisplayed()
        composeRule.onAllNodesWithText("Mercado").assertCountEquals(0)
    }
}
