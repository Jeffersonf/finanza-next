package com.finanza.next

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.finanza.next.ui.components.categoryColor
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.CategoryColors
import com.finanza.next.ui.theme.FinanzaWebCategoryColors
import com.finanza.next.ui.screens.FinanzaCalendarFormatting

class FinanzaDomainRulesTest {
    @Test
    fun calendarAmountsNormalizeSignsAndUseBrazilianCurrency() {
        assertEquals(16.0, FinanzaCalendarFormatting.amount("-R$ 16,00"), 0.001)
        assertEquals("60.270,00", FinanzaCalendarFormatting.number(60270.0))
        assertEquals("16,00", FinanzaCalendarFormatting.compactAmount("-R$ 16,00"))
    }

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
    fun copiedTextAndPixBecomeReviewableTransactions() {
        val today = LocalDate.of(2026, 7, 19)
        val parsed = FinanzaImportParser.parseText(
            "Pix recebido Joao R$ 48,50\nMercado R$ 128,90 em 18/07/2026",
            "wallet",
            today
        )

        assertEquals(2, parsed.size)
        assertEquals("income", parsed[0].type)
        assertEquals(48.50, parsed[0].amount, 0.001)
        assertEquals("expense", parsed[1].type)
        assertEquals("2026-07-18", parsed[1].date)
    }

    @Test
    fun statementPdfLinesKeepMerchantAndIgnoreTotals() {
        val parsed = FinanzaImportParser.parseText(
            "18/07/2026\nMercado Central 03/10\nR$ 128,90\nTOTAL DA FATURA R$ 128,90",
            "credit",
            LocalDate.of(2026, 7, 19)
        )

        assertEquals(1, parsed.size)
        assertEquals("Mercado Central 03/10", parsed.single().description)
        assertEquals("2026-07-18", parsed.single().date)
        assertEquals(128.90, parsed.single().amount, 0.001)
    }

    @Test
    fun nubankPdfImportsOnlyRowsInsideThePurchaseLedger() {
        val parsed = FinanzaImportParser.parseText(
            """
            FATURA 23 JUL 2026 EMISSÃO E ENVIO 16 JUL 2026
            Limite total do cartão de crédito: R$ 14.000,00
            Juros totais R$ 222,06
            TRANSAÇÕES DE 16 JUN A 16 JUL
            16 JUN
            •••• 3992 Korfit Academia - Parcela 6/12 R$ 149,00
            17 JUN
            •••• 3992 Cofesa Max Lj R$ 181,36
            24 JUN
            Estorno de "Shopee *Digitalcalotas" −R$ 82,61
            07 JUL
            Amazon BR III - NuPay R$ 23,29
            Estorno de Amazon BR III - NuPay −R$ 23,29
            Amazon BR III - NuPay R$ 23,29
            Pagamentos e Financiamentos -R$ 2.830,28
            22 JUN Pagamento em 22 JUN −R$ 2.830,28
            """.trimIndent(),
            "credit",
            LocalDate.of(2026, 7, 19)
        )

        assertEquals(3, parsed.size)
        assertEquals("Korfit Academia - Parcela 6/12", parsed[0].description)
        assertEquals("2026-06-16", parsed[0].date)
        assertEquals(149.0, parsed[0].amount, 0.001)
        assertEquals("Cofesa Max Lj", parsed[1].description)
        assertEquals(1, parsed.count { it.description == "Amazon BR III - NuPay" })
    }

    @Test
    fun nubankPdfReadsDateAndPurchaseOnTheSameLine() {
        val parsed = FinanzaImportParser.parseText(
            """
            FATURA 23 JUL 2026
            TRANSAÇÕES DE 16 JUN A 16 JUL
            Jefferson Paula R$ 3.202,82
            16 JUN •••• 3992 Korfit Academia - Parcela 6/12 R$ 149,00
            07 JUL Amazon BR III - NuPay R$ 23,29
            Pagamentos e Financiamentos -R$ 2.830,28
            """.trimIndent(),
            "credit",
            LocalDate.of(2026, 7, 23)
        )
        assertEquals(2, parsed.size)
        assertEquals("Korfit Academia - Parcela 6/12", parsed[0].description)
        assertEquals("2026-06-16", parsed[0].date)
        assertEquals(149.0, parsed[0].amount, 0.001)
        assertEquals("Amazon BR III - NuPay", parsed[1].description)
    }

    @Test
    fun nubankJulyInvoiceKeepsOnlyPurchasesAndIgnoresAdministrativeSections() {
        val parsed = FinanzaImportParser.parseText(
            """
            FATURA 23 JUL 2026 EMISSÃO E ENVIO 16 JUL 2026
            Limite total do cartão de crédito: R$ 14.000,00
            Juros totais R$ 222,06
            RESUMO DA FATURA ATUAL
            Total de compras de todos os cartões R$ 3.252,14
            Total a pagar R$ 3.202,82
            TRANSAÇÕES DE 16 JUN A 16 JUL
            Jefferson Paula R$ 3.202,82
            16 JUN •••• 3992 Korfit Academia - Parcela 6/12 R$ 149,00
            16 JUN •••• 4863 Shopee *Digitalcalotas - Parcela 2/2 R$ 85,15
            16 JUN •••• 3992 Lojas Cem F045 - Parcela 7/8 R$ 71,00
            17 JUN •••• 3992 Arenamovimentari R$ 15,00
            24 JUN Estorno de "Shopee *Digitalcalotas" −R$ 82,61
            07 JUL Amazon BR III - NuPay R$ 23,29
            07 JUL Estorno de Amazon BR III - NuPay −R$ 23,29
            07 JUL Amazon BR III - NuPay R$ 23,29
            Pagamentos e Financiamentos -R$ 2.830,28
            22 JUN Pagamento em 22 JUN −R$ 2.830,28
            """.trimIndent(),
            "credit",
            LocalDate.of(2026, 7, 23)
        )

        assertEquals(5, parsed.size)
        assertTrue(parsed.none { item ->
            item.description.contains("Jefferson Paula") ||
                item.description.contains("Pagamento") ||
                item.description.contains("Juros")
        })
        assertEquals(1, parsed.count { it.description == "Amazon BR III - NuPay" })
        assertEquals("Korfit Academia - Parcela 6/12", parsed.first().description)
        assertEquals(149.0, parsed.first().amount, 0.001)
        assertEquals(2.54, parsed.first { it.description.contains("Digitalcalotas") }.amount, 0.001)
        assertEquals(85.15, parsed.first { it.description.contains("Digitalcalotas") }.originalAmount!!, 0.001)
    }

    @Test
    fun nubankInvoiceIgnoresAdministrativeRowsEvenAfterPurchaseDateWasSeen() {
        val parsed = FinanzaImportParser.parseText(
            """
            FATURA 23 JUL 2026
            TRANSAÇÕES DE 16 JUN A 16 JUL
            16 JUN •••• 3992 Korfit Academia - Parcela 6/12 R$ 149,00
            16 JUN Limite total do cartão de crédito R$ 14.000,00
            16 JUN Total a pagar R$ 3.202,82
            22 JUN Pagamento em 22 JUN −R$ 2.830,28
            23 JUN Saldo restante da fatura anterior R$ 0,00
            """.trimIndent(),
            "credit",
            LocalDate.of(2026, 7, 23)
        )

        assertEquals(1, parsed.size)
        assertEquals("Korfit Academia - Parcela 6/12", parsed.single().description)
        assertEquals(149.0, parsed.single().amount, 0.001)
    }

    @Test
    fun nubankPdfBoxUnicodeRowsKeepCleanMerchantNamesAndRefunds() {
        val raw = listOf(
            "FATURA 23 JUL 2026",
            "TRANSA\u00c7\u00d5ES DE 16 JUN A 16 JUL",
            "16 JUN \u2022\u2022\u2022\u2022 3992 Korfit Academia - Parcela 6/12 R$ 149,00",
            "24 JUN Estorno de \"Shopee *Digitalcalotas\" \u2212R$ 82,61",
            "07 JUL Amazon BR III - NuPay R$ 23,29",
            "07 JUL Estorno de Amazon BR III - NuPay \u2212R$ 23,29",
            "07 JUL Amazon BR III - NuPay R$ 23,29",
            "Pagamentos e Financiamentos -R$ 2.830,28",
            "22 JUN Pagamento em 22 JUN \u2212R$ 2.830,28"
        ).joinToString("\n")

        val parsed = FinanzaImportParser.parseText(raw, "credit", LocalDate.of(2026, 7, 23))

        assertEquals(2, parsed.size)
        assertEquals("Korfit Academia - Parcela 6/12", parsed[0].description)
        assertEquals(149.0, parsed[0].amount, 0.001)
        assertEquals("Amazon BR III - NuPay", parsed[1].description)
    }

    @Test
    fun pixQrPayloadUsesMerchantAndAmount() {
        val parsed = FinanzaImportParser.parseText(
            "00020126330014BR.GOV.BCB.PIX0111chave-pix01520400005303986540510.005802BR5912PADARIA JOAO6009SAO PAULO62070503***6304ABCD",
            "wallet",
            LocalDate.of(2026, 7, 19)
        )

        assertEquals(1, parsed.size)
        assertEquals("expense", parsed.single().type)
        assertEquals(10.0, parsed.single().amount, 0.001)
        assertEquals("PADARIA JOAO", parsed.single().description)
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

    @Test
    fun webExperienceUsesTheWebCategoryPaletteWithoutChangingModern() {
        val category = "Mercado"
        val webIndex = Math.floorMod(FinanzaCategories.normalize(category).hashCode(), FinanzaWebCategoryColors.size)
        val modernIndex = Math.floorMod(FinanzaCategories.normalize(category).hashCode(), CategoryColors.size)

        assertEquals(FinanzaWebCategoryColors[webIndex], categoryColor(category, AppExperience.WEB))
        assertEquals(CategoryColors[modernIndex], categoryColor(category, AppExperience.NEXT))
    }

    private fun tx(type: String, amount: Double, category: String, date: String, paid: Boolean = false) =
        FinanzaPlanningTransaction(type, amount, category, LocalDate.parse(date), paid)
}
