package com.finanza.next

import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.abs

/**
 * Keeps statement reconciliation deterministic and local. A statement is never imported
 * automatically: the UI presents these findings and the person decides line by line.
 */
internal data class InvoiceExistingTransaction(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val date: String,
    val installmentGroup: String = "",
    val installmentNum: Int = 0,
    val installmentTotal: Int = 0
)

internal enum class InvoiceMatchKind { EXACT, REFUND_ADJUSTMENT, LIKELY_INSTALLMENT }

internal data class InvoiceMatch(
    val existingId: Long,
    val kind: InvoiceMatchKind,
    val title: String,
    val date: String,
    val installmentLabel: String = ""
)

internal data class InvoiceReviewLine(
    val transaction: FinanzaImportedTransaction,
    val installmentLabel: String = "",
    val matches: List<InvoiceMatch> = emptyList()
)

internal object FinanzaInvoiceReview {
    private val namedInstallmentPattern = Regex("(?i)\\b(?:parcela|parc\\.?|prestacao|prest\\.?)\\s*(\\d{1,2})\\s*(?:/|de)\\s*(\\d{1,2})\\b")
    // Card statements often omit the word "parcela" and finish the description with 03/10.
    // Keep this deliberately strict; a compact marker alone never overrides stored installment data.
    private val compactInstallmentPattern = Regex("\\b(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*$")

    fun review(
        imported: List<FinanzaImportedTransaction>,
        existing: List<InvoiceExistingTransaction>
    ): List<InvoiceReviewLine> = imported.map { incoming ->
        val installment = installmentInfo(incoming.description)
        val normalizedTitle = merchantKey(incoming.description)
        val matches = existing.mapNotNull { current ->
            if (current.type != incoming.type) return@mapNotNull null
            val sameMerchant = merchantMatches(merchantKey(current.title), normalizedTitle)
            if (!sameMerchant) return@mapNotNull null
            val sameChargeAmount = sameAmount(current.amount, incoming.amount)
            val refundAdjustment = incoming.originalAmount?.let { gross ->
                    current.type == "expense" &&
                    sameAmount(current.amount, gross) &&
                    current.date <= incoming.date
            } == true
            if (!sameChargeAmount && !refundAdjustment) return@mapNotNull null
            val currentInstallment = installmentInfo(current.title, current.installmentNum, current.installmentTotal)
            when {
                sameChargeAmount && sameDay(current.date, incoming.date) -> InvoiceMatch(
                    current.id,
                    InvoiceMatchKind.EXACT,
                    current.title,
                    current.date,
                    currentInstallment.label
                )
                refundAdjustment -> InvoiceMatch(
                    current.id,
                    InvoiceMatchKind.REFUND_ADJUSTMENT,
                    current.title,
                    current.date,
                    currentInstallment.label
                )
                hasInstallmentEvidence(installment, currentInstallment, current.installmentGroup) -> InvoiceMatch(
                    current.id,
                    InvoiceMatchKind.LIKELY_INSTALLMENT,
                    current.title,
                    current.date,
                    currentInstallment.label.ifBlank { installment.label }
                )
                else -> null
            }
        }.sortedBy { it.kind.ordinal }
        InvoiceReviewLine(incoming, installment.label, matches)
    }

    fun merchantKey(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(namedInstallmentPattern, " ")
        .replace(compactInstallmentPattern, " ")
        .replace(Regex("(?i)\\b(?:compra|lancamento|lanc\\.?|r\\$|brl|cartao|credito)\\b"), " ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun merchantMatches(first: String, second: String): Boolean {
        if (first.isBlank() || second.isBlank()) return false
        if (first == second) return true
        val shorter = if (first.length <= second.length) first else second
        val longer = if (first.length <= second.length) second else first
        return shorter.length >= 5 && longer.contains(shorter)
    }

    private fun sameAmount(first: Double, second: Double): Boolean = abs(first - second) < 0.011

    private fun sameDay(first: String, second: String): Boolean = first.take(10) == second.take(10)

    private fun installmentInfo(title: String, number: Int = 0, total: Int = 0): InstallmentInfo {
        if (number > 0 && total > 0) return InstallmentInfo(number, total)
        namedInstallmentPattern.find(title)?.let { match ->
            return InstallmentInfo(match.groupValues[1].toIntOrNull() ?: 0, match.groupValues[2].toIntOrNull() ?: 0, explicit = true)
        }
        compactInstallmentPattern.find(title)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: 0
            val total = match.groupValues[2].toIntOrNull() ?: 0
            if (number in 1..total && total >= 2) return InstallmentInfo(number, total, explicit = true)
        }
        return InstallmentInfo()
    }

    private fun hasInstallmentEvidence(
        incoming: InstallmentInfo,
        existing: InstallmentInfo,
        existingGroup: String
    ): Boolean {
        // A same-merchant, same-value charge is not enough to label a line as an
        // installment. Statements normally carry a 03/10-style marker; when both
        // sides have it, the merchant, amount and total number of installments
        // are strong evidence of the same planned series. The installment number
        // may differ because a future row can already be one or more cycles ahead.
        if (!incoming.explicit) return false
        if (!existing.explicit && existingGroup.isBlank() && existing.total == 0) return false
        if (existing.number == 0 || existing.total == 0) return existingGroup.isNotBlank()
        return incoming.total == existing.total
    }

    private data class InstallmentInfo(val number: Int = 0, val total: Int = 0, val explicit: Boolean = false) {
        val label: String get() = if (number > 0 && total > 0) "$number/$total" else ""
    }
}
