package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.navigation.AppActions
import com.finanza.next.navigation.AppScaffold
import com.finanza.next.navigation.AppUiState
import com.finanza.next.ui.components.AppTab
import com.finanza.next.ui.screens.ConfigActions
import com.finanza.next.ui.screens.ConfigUiState
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test

class ExperienceThemeRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appScaffoldRendersInNextLight() = render(AppExperience.NEXT, dark = false)

    @Test
    fun appScaffoldRendersInNextDark() = render(AppExperience.NEXT, dark = true)

    @Test
    fun appScaffoldRendersInFinanzaLight() = render(AppExperience.FINANZA, dark = false)

    @Test
    fun appScaffoldRendersInFinanzaDark() = render(AppExperience.FINANZA, dark = true)

    @Test
    fun finanzaDashboardUsesWebSummaryWithoutManagerStrip() {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.FINANZA) {
                AppScaffold(emptyState(), emptyActions())
            }
        }

        listOf("Saldo total", "Receitas", "Despesas", "A pagar", "Início").forEach { label ->
            composeRule.onNodeWithText(label, ignoreCase = true).assertIsDisplayed()
        }
        check(composeRule.onAllNodesWithText("Widgets do painel").fetchSemanticsNodes().isEmpty())
    }

    private fun render(experience: AppExperience, dark: Boolean) {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = dark, experience = experience) {
                AppScaffold(emptyState(), emptyActions())
            }
        }

        val title = if (experience == AppExperience.FINANZA) "Dashboard" else "Voce"
        composeRule.onAllNodesWithText(title)[0].assertIsDisplayed()
    }

    private fun emptyState() = AppUiState(
        selectedTab = AppTab.HOME,
        userName = "Voce",
        greeting = "Ola",
        period = "Julho",
        balance = "R$ 0,00",
        accountsTotal = "R$ 0,00",
        income = "R$ 0,00",
        spent = "R$ 0,00",
        paid = "R$ 0,00",
        remaining = "R$ 0,00",
        billProgress = 0f,
        transactions = emptyList(),
        accounts = emptyList(),
        bills = emptyList(),
        categories = emptyList(),
        monthlyTrends = emptyList(),
        config = ConfigUiState(
            userName = "Voce",
            accountStatus = "Modo local",
            budget = "R$ 0,00",
            theme = "Next / Claro",
            accounts = 0,
            currency = "BRL - R$",
            notifications = false,
            privacy = false,
            lastSync = "Nunca",
            pendingSync = 0,
            syncError = "",
            role = "",
            twoFactor = false
        ),
        paymentMethods = emptyList(),
        features = FeatureCenterUiState(emptyList(), 0, false)
    )

    private fun emptyActions() = AppActions(
        selectTab = {},
        refresh = { finished -> finished() },
        openTransaction = {},
        openAccount = {},
        openBill = {},
        newAccount = {},
        transferAccounts = {},
        saveExpense = { _, _, _, _ -> true },
        config = ConfigActions(
            editProfile = {},
            openAccount = {},
            editBudget = {},
            changeTheme = {},
            toggleNotifications = {},
            togglePrivacy = {},
            sync = {},
            backup = {},
            diagnostics = {},
            security = {},
            admin = {},
            pinShortcut = {},
            clearData = {}
        ),
        features = com.finanza.next.features.FeatureActions(
            save = { _, _, _ -> true },
            delete = { _, _ -> },
            primary = { _, _ -> },
            secondary = { _, _ -> },
            importBackup = {},
            importTransactions = {},
            exportBackup = {},
            sync = {}
        )
    )
}
