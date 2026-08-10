package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.finanza.next.ui.screens.OnboardingScreen
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingExplainsDashboardModulesQuickEntryAndWidget() {
        var completed = false
        var widgetRequested = false
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = AppExperience.NEXT) {
                OnboardingScreen(
                    onComplete = { completed = true },
                    canPinWidget = true,
                    onRequestWidgetPin = { widgetRequested = true }
                )
            }
        }

        composeRule.onNodeWithText("Seu dinheiro em um s\u00F3 lugar").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("M\u00F3dulos quando precisar").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Registre sem perder tempo").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Deixe a captura na tela inicial").assertIsDisplayed()
        composeRule.onNodeWithText("Gasto r\u00E1pido").assertIsDisplayed()
        composeRule.onNodeWithText("Adicionar widget").performClick()
        assertTrue(completed)
        assertTrue(widgetRequested)
    }
}
