package com.finanza.next

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanzaDomainRulesTest {
    @Test
    fun csvAndOfxFollowTheFinancialContract() {
        val csv = "Data;Descricao;Valor;Tipo\n18/07/2026;Mercado;1.234,56;Saida\n19/07/2026;Salario;5000,00;Entrada"
        val ofx = "<OFX><STMTTRN><DTPOSTED>20260718<TRNAMT>-42.50<FITID>x1<NAME>Padaria</STMTTRN></OFX>"

        val csvItems = FinanzaImportParser.parseCsv(csv, "wallet", LocalDate.of(2026, 7, 19))
        val ofxItems = FinanzaImportParser.parseOfx(ofx, "wallet", LocalDate.of(2026, 7, 19))

        assertEquals(2, csvItems.size)
        assertEquals(1234.56, csvItems[0].amount, 0.001)
        assertEquals("expense", csvItems[0].type)
        assertEquals("income", csvItems[1].type)
        assertEquals("x1", ofxItems.single().sourceId)
        assertEquals("expense", ofxItems.single().type)
    }

    @Test
    fun budgetAndTrendsMatchWebRules() {
        val today = LocalDate.of(2026, 7, 19)
        val items = listOf(
            tx("expense", 100.0, "Mercado", "2026-07-10"),
            tx("expense", 30.0, "Mercado", "2026-07-20"),
            tx("expense", 20.0, "Mercado", "2026-07-11", true),
            tx("expense", 500.0, "Transferencia", "2026-07-12"),
            tx("income", 1000.0, "Renda", "2026-06-01")
        )

        val spending = FinanzaPlanningRules.budgetSpending(items, YearMonth.of(2026, 7), today)
        val trends = FinanzaPlanningRules.monthTotals(items, YearMonth.of(2026, 7), 2)

        assertEquals(mapOf("Mercado" to 100.0), spending)
        assertEquals(1000.0, trends[0].income, 0.001)
        assertEquals(150.0, trends[1].expense, 0.001)
    }

    private fun tx(type: String, amount: Double, category: String, date: String, paid: Boolean = false) =
        FinanzaPlanningTransaction(type, amount, category, LocalDate.parse(date), paid)
}
