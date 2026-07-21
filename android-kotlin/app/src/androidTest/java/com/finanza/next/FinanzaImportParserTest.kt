package com.finanza.next

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaImportParserTest {
    private val today = LocalDate.of(2026, 7, 19)

    @Test
    fun csvSupportsBrazilianValuesAccentsAndQuotedNewlines() {
        val csv = """Data;Descrição;Valor;Tipo;Categoria
            |18/07/2026;"Mercado
            |do bairro";"R$ 1.234,56";Saída;Mercado
            |19/07/2026;Salário;5000,00;Entrada;Renda
        """.trimMargin()

        val parsed = FinanzaImportParser.parseCsv(csv, "account", today)

        assertEquals(2, parsed.size)
        assertEquals("Mercado\ndo bairro", parsed[0].description)
        assertEquals(1234.56, parsed[0].amount, 0.001)
        assertEquals("expense", parsed[0].type)
        assertEquals("2026-07-18", parsed[0].date)
        assertEquals("income", parsed[1].type)
    }

    @Test
    fun ofxKeepsFitIdStableAndTransactionSign() {
        val ofx = """
            <OFX><BANKTRANLIST>
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260718120000<TRNAMT>-42.50<FITID>abc-1<NAME>Padaria</STMTTRN>
            <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260719<TRNAMT>100.00<FITID>abc-2<MEMO>Reembolso</STMTTRN>
            </BANKTRANLIST></OFX>
        """.trimIndent()

        val first = FinanzaImportParser.parseOfx(ofx, "account", today)
        val second = FinanzaImportParser.parseOfx(ofx, "account", today)

        assertEquals(2, first.size)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals("expense", first[0].type)
        assertEquals("income", first[1].type)
        assertEquals("abc-1", first[0].sourceId)
    }
}
