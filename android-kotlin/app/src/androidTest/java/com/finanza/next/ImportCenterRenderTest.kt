package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finanza.next.features.FeatureActions
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.ui.screens.FeatureCenterScreen
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ImportCenterRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun freezeTestClock() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun directImportCenterOffersStatementReviewActions() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.WEB) {
                FeatureCenterScreen(
                    state = FeatureCenterUiState(emptyList(), pendingSync = 0, online = true),
                    actions = actions(),
                    initialImportCenter = true,
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Importar dados").assertIsDisplayed()
        composeRule.onNodeWithText("Importar PDF ou extrato").assertIsDisplayed()
        composeRule.onNodeWithText("Importar CSV ou OFX").assertIsDisplayed()
    }

    private fun actions() = FeatureActions(
        save = { _, _, _ -> true },
        delete = { _, _ -> },
        primary = { _, _ -> },
        secondary = { _, _ -> },
        importBackup = {},
        importTransactions = {},
        importPdf = {},
        importText = {},
        exportBackup = {},
        sync = {}
    )
}
