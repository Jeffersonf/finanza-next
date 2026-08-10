package com.finanza.next

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanzaInvoiceReviewUnitTest {
    @Test
    fun exactStatementPurchaseIsBlocked() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Mercado Central", "2026-08-02", 89.90)),
            existing = listOf(existing(1, "Mercado Central", "2026-08-02", 89.90))
        )

        assertEquals(InvoiceMatchKind.EXACT, lines.single().matches.single().kind)
    }

    @Test
    fun matchingFutureInstallmentNeedsManualReview() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Notebook Parcela 3/10", "2026-08-02", 219.90)),
            existing = listOf(existing(42, "Notebook parcela 3/10", "2026-09-02", 219.90, 3, 10))
        )

        assertEquals(InvoiceMatchKind.LIKELY_INSTALLMENT, lines.single().matches.single().kind)
        assertEquals("3/10", lines.single().installmentLabel)
    }

    @Test
    fun differentMerchantDoesNotCreateAFalseInstallmentMatch() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Farmacia Bairro 2/6", "2026-08-02", 50.0)),
            existing = listOf(existing(7, "Academia Centro 2/6", "2026-09-02", 50.0, 2, 6))
        )

        assertTrue(lines.single().matches.isEmpty())
        assertFalse(FinanzaInvoiceReview.merchantKey("Farmácia Bairro 2/6").contains("academia"))
    }

    @Test
    fun compactStatementInstallmentMatchesAPlannedInstallment() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Notebook Gamer 03/10", "2026-08-02", 219.90)),
            existing = listOf(existing(42, "Notebook Gamer parcela 3/10", "2026-09-02", 219.90, 3, 10))
        )

        assertEquals(InvoiceMatchKind.LIKELY_INSTALLMENT, lines.single().matches.single().kind)
        assertEquals("3/10", lines.single().installmentLabel)
    }

    @Test
    fun shortenedMerchantNameStillFlagsTheSameInstallment() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Korfit 4/12", "2026-08-02", 149.00)),
            existing = listOf(existing(42, "Korfit Academia - Parcela 4/12", "2026-09-02", 149.00, 4, 12))
        )

        assertEquals(InvoiceMatchKind.LIKELY_INSTALLMENT, lines.single().matches.single().kind)
        assertEquals("4/12", lines.single().matches.single().installmentLabel)
    }

    @Test
    fun partialRefundCanAdjustAnExistingGrossCharge() {
        val incoming = FinanzaImportedTransaction(
            id = 100,
            description = "Shopee Digitalcalotas",
            category = "A classificar",
            amount = 2.54,
            type = "expense",
            date = "2026-06-24",
            accountId = "credit",
            originalAmount = 85.15
        )
        val line = FinanzaInvoiceReview.review(
            imported = listOf(incoming),
            existing = listOf(existing(9, "Shopee Digitalcalotas", "2026-06-16", 85.15))
        ).single()

        assertEquals(InvoiceMatchKind.REFUND_ADJUSTMENT, line.matches.single().kind)
        assertEquals(9L, line.matches.single().existingId)
    }

    @Test
    fun sameInstallmentSeriesWithDifferentNumberIsFlaggedForReview() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Korfit Academia - Parcela 6/12", "2026-07-16", 149.00)),
            existing = listOf(existing(42, "Korfit Academia - Parcela 4/12", "2026-08-16", 149.00, 4, 12))
        )

        assertEquals(InvoiceMatchKind.LIKELY_INSTALLMENT, lines.single().matches.single().kind)
        assertEquals("4/12", lines.single().matches.single().installmentLabel)
    }

    @Test
    fun matchingMerchantWithoutAnInstallmentMarkerStaysNew() {
        val lines = FinanzaInvoiceReview.review(
            imported = listOf(imported("Notebook Gamer", "2026-08-02", 219.90)),
            existing = listOf(existing(42, "Notebook Gamer parcela 3/10", "2026-09-02", 219.90, 3, 10))
        )

        assertTrue(lines.single().matches.isEmpty())
    }

    private fun imported(description: String, date: String, amount: Double) = FinanzaImportedTransaction(
        id = description.hashCode().toLong(),
        description = description,
        category = "A classificar",
        amount = amount,
        type = "expense",
        date = date,
        accountId = "credit"
    )

    private fun existing(id: Long, title: String, date: String, amount: Double, installment: Int = 0, total: Int = 0) =
        InvoiceExistingTransaction(id, title, amount, "expense", date, "card", installment, total)
}
