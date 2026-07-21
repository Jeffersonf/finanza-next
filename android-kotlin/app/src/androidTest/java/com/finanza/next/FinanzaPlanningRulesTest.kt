package com.finanza.next

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaPlanningRulesTest {
    @Test
    fun budgetExcludesFuturePaidAndTransfers() {
        val today = LocalDate.of(2026, 7, 19)
        val transactions = listOf(
            tx("expense", 100.0, "Mercado", "2026-07-10"),
            tx("expense", 30.0, "Mercado", "2026-07-20"),
            tx("expense", 20.0, "Mercado", "2026-07-11", paid = true),
            tx("expense", 500.0, "Transferencia", "2026-07-12")
        )

        val spending = FinanzaPlanningRules.budgetSpending(transactions, YearMonth.of(2026, 7), today)

        assertEquals(mapOf("Mercado" to 100.0), spending)
    }

    @Test
    fun monthTotalsIgnoreTransfersAndKeepChronologicalWindow() {
        val transactions = listOf(
            tx("income", 1000.0, "Renda", "2026-06-01"),
            tx("expense", 200.0, "Mercado", "2026-07-01"),
            tx("expense", 50.0, "Transferencia", "2026-07-02")
        )

        val totals = FinanzaPlanningRules.monthTotals(transactions, YearMonth.of(2026, 7), 2)

        assertEquals(YearMonth.of(2026, 6), totals[0].month)
        assertEquals(1000.0, totals[0].income, 0.001)
        assertEquals(200.0, totals[1].expense, 0.001)
    }

    private fun tx(type: String, amount: Double, category: String, date: String, paid: Boolean = false) =
        FinanzaPlanningTransaction(type, amount, category, LocalDate.parse(date), paid)
}
