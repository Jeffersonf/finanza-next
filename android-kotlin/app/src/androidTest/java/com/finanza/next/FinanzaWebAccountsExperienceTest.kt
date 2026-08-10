package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.screens.ContasScreen
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test

class FinanzaWebAccountsExperienceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun webAccountsExposeCardCenterDetails() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.WEB) {
                ContasScreen(
                    period = "AGO",
                    total = "R$ 1.200,00",
                    progress = 0.4f,
                    paid = "R$ 400,00",
                    remaining = "R$ 600,00",
                    accounts = listOf(
                        AccountUi(
                            id = "card",
                            name = "Nubank",
                            type = "Cartão",
                            amount = "R$ 0,00",
                            iconKey = "credit",
                            cardClosingDay = 12,
                            cardDueDay = 20,
                            cardLast4 = "1234",
                            cardExpiry = "08/29"
                        )
                    ),
                    bills = emptyList(),
                    onNewAccount = {},
                    onTransfer = {},
                    onAccount = {},
                    onBill = {}
                )
            }
        }

        composeRule.onNodeWithText("Central de cartões").assertIsDisplayed()
        composeRule.onNodeWithText("Fecha 12").assertIsDisplayed()
        composeRule.onNodeWithText("Vence 20").assertIsDisplayed()
        composeRule.onNodeWithText("final 1234 • validade 08/29").assertIsDisplayed()
        composeRule.onNodeWithText("Compromissos").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum vencimento").assertIsDisplayed()
    }
}
