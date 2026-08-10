package com.finanza.next

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.finanza.next.ui.screens.FinanceCalendarEventUi
import com.finanza.next.ui.screens.FinanceCalendarScreen
import com.finanza.next.ui.screens.FinanceCalendarSource
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FinanceCalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun webCalendarShowsCurrentMonthEventAndDueSummary() {
        val today = LocalDate.now()
        render(AppExperience.WEB, today)

        composeRule.onNodeWithText("Agenda").assertIsDisplayed()
        composeRule.onNodeWithText("Mercado").assertIsDisplayed()
        composeRule.onNodeWithText("1 vencimento nesta janela").assertIsDisplayed()
    }

    @Test
    fun modernCalendarOpensTransactionFromItsPill() {
        val today = LocalDate.now()
        var clickedId = 0L
        render(AppExperience.NEXT, today) { clickedId = it.id }

        composeRule.onNodeWithTag("calendarEvent-42").assertIsDisplayed().performClick()
        assertEquals(42L, clickedId)
    }

    private fun render(
        experience: AppExperience,
        date: LocalDate,
        onEvent: (FinanceCalendarEventUi) -> Unit = {}
    ) {
        val events = listOf(
            FinanceCalendarEventUi(
                id = 42L,
                title = "Mercado",
                amount = "R$ 120,00",
                date = date,
                category = "Alimentação",
                color = Color(0xFF5A9EF5),
                income = false,
                source = FinanceCalendarSource.TRANSACTION
            ),
            FinanceCalendarEventUi(
                id = 99L,
                title = "Aluguel",
                amount = "R$ 900,00",
                date = date,
                category = "Moradia",
                color = Color(0xFFF5705A),
                income = false,
                source = FinanceCalendarSource.DUE
            )
        )
        composeRule.setContent {
            FinanceAppTheme(darkTheme = true, experience = experience) {
                FinanceCalendarScreen(
                    events = events,
                    onEvent = onEvent,
                    onQuickAdd = {},
                    onClose = {}
                )
            }
        }
    }
}
