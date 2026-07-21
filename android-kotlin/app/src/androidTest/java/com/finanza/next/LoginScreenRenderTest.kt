package com.finanza.next

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finanza.next.ui.screens.LoginScreen
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginRendersInNextLight() = render(AppExperience.NEXT, dark = false)

    @Test
    fun loginRendersInNextDark() = render(AppExperience.NEXT, dark = true)

    @Test
    fun loginRendersInFinanzaLight() = render(AppExperience.FINANZA, dark = false)

    @Test
    fun loginRendersInFinanzaDark() = render(AppExperience.FINANZA, dark = true)

    private fun render(experience: AppExperience, dark: Boolean) {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = dark, experience = experience) {
                LoginScreen(
                    initialUrl = "https://api.example.test",
                    initialUsername = "jefferson",
                    busy = false,
                    error = null,
                    onLogin = { _, _, _, _ -> },
                    onContinueOffline = {}
                )
            }
        }

        composeRule.onNodeWithText("Finext").assertIsDisplayed()
        composeRule.onNodeWithText("Entrar e sincronizar").assertIsDisplayed()
        composeRule.onNodeWithText("Usar somente neste aparelho").assertIsDisplayed()
    }
}
