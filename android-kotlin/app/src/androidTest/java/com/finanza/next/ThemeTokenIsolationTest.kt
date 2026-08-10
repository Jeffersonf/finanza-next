package com.finanza.next

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test

class ThemeTokenIsolationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun modernDarkKeepsBlackBackgroundAndItsOwnAccent() {
        render(AppExperience.NEXT, dark = true)

        composeRule.onNodeWithText("experience=NEXT").assertIsDisplayed()
        composeRule.onNodeWithText("background=FF000000").assertIsDisplayed()
        composeRule.onNodeWithText("accent=FFF7F7FA").assertIsDisplayed()
    }

    @Test
    fun webLightUsesWebBackgroundAndAccent() {
        render(AppExperience.WEB, dark = false)

        composeRule.onNodeWithText("experience=WEB").assertIsDisplayed()
        composeRule.onNodeWithText("background=FFF4F7EF").assertIsDisplayed()
        composeRule.onNodeWithText("accent=FF3F7D00").assertIsDisplayed()
    }

    @Test
    fun webDarkDoesNotFallBackToModernBlackAccent() {
        render(AppExperience.WEB, dark = true)

        composeRule.onNodeWithText("experience=WEB").assertIsDisplayed()
        composeRule.onNodeWithText("background=FF08090D").assertIsDisplayed()
        composeRule.onNodeWithText("accent=FFC8F55A").assertIsDisplayed()
    }

    @Composable
    private fun ThemeProbe(experience: AppExperience) {
        Text("experience=${experience.name}")
        Text("background=${hex(MaterialTheme.colorScheme.background.toArgb())}")
        Text("accent=${hex(MaterialTheme.colorScheme.primary.toArgb())}")
    }

    private fun render(experience: AppExperience, dark: Boolean) {
        composeRule.setContent {
            FinanceAppTheme(darkTheme = dark, experience = experience) {
                ThemeProbe(experience)
            }
        }
    }

    private fun hex(value: Int): String = String.format(Locale.US, "%08X", value)
}
