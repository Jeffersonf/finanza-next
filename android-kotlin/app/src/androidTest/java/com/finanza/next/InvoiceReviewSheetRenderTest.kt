package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.finanza.next.ui.screens.InvoiceReviewSheet
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test

class InvoiceReviewSheetRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plannedInstallmentRequiresConfirmationBeforeRemoval() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.NEXT) {
                InvoiceReviewSheet(
                    lines = listOf(
                        InvoiceReviewLine(
                            transaction = FinanzaImportedTransaction(
                                id = 1,
                                description = "Notebook 03/10",
                                category = "A classificar",
                                amount = 219.90,
                                type = "expense",
                                date = "2026-08-03",
                                accountId = "credit"
                            ),
                            installmentLabel = "3/10",
                            matches = listOf(
                                InvoiceMatch(
                                    existingId = 42,
                                    kind = InvoiceMatchKind.LIKELY_INSTALLMENT,
                                    title = "Notebook 03/10",
                                    date = "2026-09-03",
                                    installmentLabel = "3/10"
                                )
                            )
                        )
                    ),
                    onDismiss = {},
                    onRemovePlannedInstallment = {},
                    onConfirm = {}
                )
            }
        }

        composeRule.onNodeWithText("Remover parcela planejada").performClick()
        composeRule.onNodeWithText("Remover parcela planejada?").assertIsDisplayed()
        composeRule.onNodeWithText("Manter").assertIsDisplayed()
        composeRule.onNodeWithText("Remover").assertIsDisplayed()
    }
}
