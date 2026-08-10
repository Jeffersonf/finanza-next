package com.finanza.next

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

internal data class FinanzaImportedTransaction(
    val id: Long,
    val description: String,
    val category: String,
    val amount: Double,
    val type: String,
    val date: String,
    val accountId: String,
    val sourceId: String = "",
    val originalAmount: Double? = null
)

internal object FinanzaImportParser {
    fun parse(raw: String, accountId: String, today: LocalDate = LocalDate.now()): List<FinanzaImportedTransaction> =
        if (raw.contains("<OFX", ignoreCase = true) || raw.contains("<STMTTRN>", ignoreCase = true)) {
            parseOfx(raw, accountId, today)
        } else {
            parseCsv(raw, accountId, today)
        }

    fun parseOfx(raw: String, accountId: String, today: LocalDate = LocalDate.now()): List<FinanzaImportedTransaction> {
        val blocks = Regex(
            "<STMTTRN>(.*?)(?=</STMTTRN>|<STMTTRN>|</BANKTRANLIST>|</CCSTMTRS>)",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(raw)
        return blocks.mapNotNull { match ->
            val block = match.groupValues[1]
            fun tag(name: String) = Regex("<$name>([^<\\r\\n]+)", RegexOption.IGNORE_CASE)
                .find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val signed = parseAmount(tag("TRNAMT")) ?: return@mapNotNull null
            val sourceId = tag("FITID")
            val rawDate = tag("DTPOSTED").filter(Char::isDigit).take(8)
            val date = if (rawDate.length == 8) {
                "${rawDate.take(4)}-${rawDate.substring(4, 6)}-${rawDate.takeLast(2)}"
            } else today.toString()
            imported(
                stableId(sourceId.ifBlank { "$date|$signed|${tag("NAME")}|${tag("MEMO")}" }),
                tag("NAME").ifBlank { tag("MEMO") }.ifBlank { "Importado OFX" },
                "A classificar", signed, date, accountId, sourceId
            )
        }.toList()
    }

    fun parseCsv(raw: String, accountId: String, today: LocalDate = LocalDate.now()): List<FinanzaImportedTransaction> {
        val records = parseRecords(raw).filter { record -> record.any(String::isNotBlank) }
        if (records.size < 2) return emptyList()
        val headers = records.first().map(::normalizeHeader)
        fun column(vararg names: String) = headers.indexOfFirst { header -> names.any { header.contains(it) } }
        val dateIndex = column("data", "date")
        val descriptionIndex = column("descricao", "description", "historico", "lancamento", "nome", "memo")
        val amountIndex = column("valor", "amount", "total", "quantia")
        val typeIndex = column("tipo", "type", "natureza")
        val categoryIndex = column("categoria", "category")
        val idIndex = column("fitid", "identificador", "transaction id", "id")
        if (amountIndex < 0) return emptyList()
        return records.drop(1).mapIndexedNotNull { index, cells ->
            val signed = parseAmount(cells.getOrNull(amountIndex).orEmpty()) ?: return@mapIndexedNotNull null
            if (signed == 0.0) return@mapIndexedNotNull null
            val explicitType = normalizeHeader(cells.getOrNull(typeIndex).orEmpty())
            val sourceId = cells.getOrNull(idIndex).orEmpty().trim()
            val description = cells.getOrNull(descriptionIndex).orEmpty().trim().ifBlank { "Importado CSV" }
            val date = parseDate(cells.getOrNull(dateIndex).orEmpty(), today)
            val typeSign = when {
                explicitType.contains("receita") || explicitType.contains("income") || explicitType.contains("entrada") -> abs(signed)
                explicitType.contains("gasto") || explicitType.contains("expense") || explicitType.contains("saida") -> -abs(signed)
                else -> signed
            }
            imported(
                stableId(sourceId.ifBlank { "$date|$typeSign|$description|$index" }),
                description,
                cells.getOrNull(categoryIndex).orEmpty().trim().ifBlank { "A classificar" },
                typeSign, date, accountId, sourceId
            )
        }
    }

    fun parseText(raw: String, accountId: String, today: LocalDate = LocalDate.now()): List<FinanzaImportedTransaction> {
        val lines = raw.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.any(::isNubankTransactionHeader)) return parseNubankStatement(lines, accountId, today)
        return lines.mapIndexedNotNull { index, line ->
            if (isStatementSummary(line)) return@mapIndexedNotNull null
            parseTextLine(statementLineWithContext(lines, index), accountId, today, index)
        }
    }

    /**
     * Nubank's PDF contains many money values outside the purchase ledger: limits, invoice
     * alternatives, interest simulations and payments. Only the explicit transaction section
     * represents card purchases and is safe to offer in the reconciliation screen.
     */
    private fun parseNubankStatement(
        lines: List<String>,
        accountId: String,
        today: LocalDate
    ): List<FinanzaImportedTransaction> {
        val referenceDate = nubankStatementDate(lines) ?: today
        val transactions = mutableListOf<FinanzaImportedTransaction>()
        var inTransactionSection = false
        var transactionDate: LocalDate? = null

        lines.forEachIndexed { index, line ->
            if (isNubankTransactionHeader(line)) {
                inTransactionSection = true
                return@forEachIndexed
            }
            if (!inTransactionSection) return@forEachIndexed

            val normalized = normalizeHeader(line)
            if (normalized.startsWith("pagamentos e financiamentos") || normalized.startsWith("em cumprimento")) {
                inTransactionSection = false
                return@forEachIndexed
            }

            val datePrefix = nubankDatePrefixPattern.find(line)
            if (datePrefix != null) {
                transactionDate = nubankDate(
                    datePrefix.groupValues[1].toIntOrNull(),
                    datePrefix.groupValues[2],
                    referenceDate.year
                )
                // Some PDF extractors put the date on its own line; a real
                // purchase row continues after the date and must be parsed.
                if (line.removeRange(datePrefix.range).trim().isBlank()) return@forEachIndexed
            }
            val activeDate = transactionDate ?: return@forEachIndexed
            val transactionLine = datePrefix?.let { line.removeRange(it.range) } ?: line
            val amountMatch = amountPattern.find(transactionLine) ?: return@forEachIndexed
            val amount = parseAmount(amountMatch.value) ?: return@forEachIndexed
            if (amount <= 0.0) return@forEachIndexed

            val description = transactionLine.removeRange(amountMatch.range)
                .replace(Regex("^[\\u2022*\\s]+"), "")
                .replace('\u2212', '-')
                .replace(Regex("^[•*\\s]+"), "")
                .replace(Regex("^(?:\\d{4}\\s+)+"), "")
                .replace(Regex("\\s+"), " ")
                .trim().trim('-', ':', '|', '−')
                .replace(Regex("(?i)\\s*[−-]?\\s*R\\$\\s*$"), "")
                .trim().trim('-', ':', '|', '−')
            if (description.isBlank()) return@forEachIndexed

            if (normalized.contains("estorno")) {
                val reversedMerchant = description.replace(Regex("(?i)^estorno\\s+(?:de\\s+)?"), "").trim()
                val reversedKey = FinanzaInvoiceReview.merchantKey(reversedMerchant)
                val previousIndex = transactions.indexOfLast { current ->
                    current.date <= activeDate.toString() &&
                        current.amount + 0.011 >= amount &&
                        merchantKeysOverlap(FinanzaInvoiceReview.merchantKey(current.description), reversedKey)
                }
                if (previousIndex >= 0) {
                    val previous = transactions[previousIndex]
                    val remaining = previous.amount - amount
                    if (remaining < 0.011) {
                        transactions.removeAt(previousIndex)
                    } else {
                        transactions[previousIndex] = previous.copy(
                            amount = remaining,
                            originalAmount = previous.originalAmount ?: previous.amount
                        )
                    }
                }
                return@forEachIndexed
            }
            if (isNubankNonPurchase(normalized)) return@forEachIndexed

            transactions += imported(
                stableId("nubank|$activeDate|$amount|$description|$index"),
                description,
                "A classificar",
                -abs(amount),
                activeDate.toString(),
                accountId,
                "nubank-pdf"
            )
        }
        return transactions
    }

    private fun parseTextLine(line: String, accountId: String, today: LocalDate, index: Int): FinanzaImportedTransaction? {
        parsePixQr(line, accountId, today, index)?.let { return it }
        val amountMatch = amountPattern.find(line) ?: return null
        val signed = parseAmount(amountMatch.value) ?: return null
        if (signed == 0.0) return null
        val normalized = normalizeHeader(line)
        val isIncome = incomeKeywords.any(normalized::contains)
        val date = datePattern.find(line)?.value?.let { parseDate(it, today) } ?: today.toString()
        val description = line.removeRange(amountMatch.range)
            .replace(datePattern, " ")
            .replace(Regex("(?i)\\b(?:pix|recebido|recebi|enviado|pago|pagamento|r\\$)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim().trim('-', ':', '|')
            .ifBlank { if (isIncome) "Receita importada" else "Despesa importada" }
        val typeSign = if (isIncome) abs(signed) else -abs(signed)
        return imported(
            stableId("$date|$typeSign|$description|$index"),
            description,
            "A classificar",
            typeSign,
            date,
            accountId,
            ""
        )
    }

    private fun statementLineWithContext(lines: List<String>, index: Int): String {
        val line = lines[index]
        if (!amountPattern.matches(line.trim())) return line

        val previous = lines.getOrNull(index - 1).orEmpty()
        if (previous.isBlank() || isStatementSummary(previous)) return line
        if (datePattern.containsMatchIn(previous)) return "$previous $line"

        val dateLine = lines.getOrNull(index - 2).orEmpty()
        return if (datePattern.containsMatchIn(dateLine) && !isStatementSummary(dateLine)) {
            "$dateLine $previous $line"
        } else {
            "$previous $line"
        }
    }

    private fun isStatementSummary(line: String): Boolean {
        val normalized = normalizeHeader(line)
        return statementSummaryKeywords.any(normalized::contains)
    }

    private fun isNubankTransactionHeader(line: String): Boolean =
        normalizeHeader(line).startsWith("transacoes de ")

    private fun nubankStatementDate(lines: List<String>): LocalDate? = lines.firstNotNullOfOrNull { line ->
        nubankFullDatePattern.find(line)?.let { match ->
            nubankDate(match.groupValues[1].toIntOrNull(), match.groupValues[2], match.groupValues[3].toIntOrNull())
        }
    }

    private fun nubankDate(day: Int?, monthText: String, year: Int?): LocalDate? {
        val month = nubankMonth(monthText) ?: return null
        val safeDay = day ?: return null
        val safeYear = year ?: return null
        return runCatching { LocalDate.of(safeYear, month, safeDay) }.getOrNull()
    }

    private fun nubankMonth(raw: String): Int? = when (normalizeHeader(raw).take(3)) {
        "jan" -> 1; "fev" -> 2; "mar" -> 3; "abr" -> 4; "mai" -> 5; "jun" -> 6
        "jul" -> 7; "ago" -> 8; "set" -> 9; "out" -> 10; "nov" -> 11; "dez" -> 12
        else -> null
    }

    private fun isNubankNonPurchase(normalized: String): Boolean = nubankNonPurchaseKeywords.any(normalized::contains)

    private fun merchantKeysOverlap(first: String, second: String): Boolean =
        first.isNotBlank() && second.isNotBlank() && (first == second || first.contains(second) || second.contains(first))

    private fun parsePixQr(line: String, accountId: String, today: LocalDate, index: Int): FinanzaImportedTransaction? {
        val compact = line.trim().replace("\r", "").replace("\n", "")
        if (!compact.startsWith("000201") || compact.length < 20) return null
        val fields = mutableMapOf<String, String>()
        var cursor = 0
        while (cursor + 4 <= compact.length) {
            val id = compact.substring(cursor, cursor + 2)
            val length = compact.substring(cursor + 2, cursor + 4).toIntOrNull() ?: break
            val next = cursor + 4 + length
            if (next > compact.length) break
            fields.putIfAbsent(id, compact.substring(cursor + 4, next))
            cursor = next
        }
        val amount = parseAmount(fields["54"].orEmpty()) ?: return null
        if (amount <= 0.0) return null
        val merchant = fields["59"].orEmpty().trim().ifBlank { "Pagamento Pix" }
        return imported(
            stableId("pix|$today|$amount|$merchant|$index"),
            merchant,
            "A classificar",
            -abs(amount),
            today.toString(),
            accountId,
            "pix-qr"
        )
    }

    private fun imported(id: Long, description: String, category: String, signed: Double, date: String, accountId: String, sourceId: String) =
        FinanzaImportedTransaction(id, description, category, abs(signed), if (signed >= 0.0) "income" else "expense", date, accountId, sourceId)

    private fun parseRecords(raw: String): List<List<String>> {
        val firstLine = raw.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val separator = if (firstLine.count { it == ';' } >= firstLine.count { it == ',' }) ';' else ','
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            when {
                char == '"' && quoted && index + 1 < raw.length && raw[index + 1] == '"' -> { cell.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == separator && !quoted -> { row += cell.toString(); cell.clear() }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < raw.length && raw[index + 1] == '\n') index++
                    row += cell.toString(); cell.clear()
                    if (row.any(String::isNotBlank)) rows += row.toList()
                    row.clear()
                }
                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            if (row.any(String::isNotBlank)) rows += row.toList()
        }
        return rows
    }

    private fun parseAmount(raw: String): Double? {
        val clean = raw.replace("R$", "", ignoreCase = true).replace(" ", "").trim()
        if (clean.isBlank()) return null
        val negativeByParentheses = clean.startsWith('(') && clean.endsWith(')')
        val unsigned = clean.removePrefix("(").removeSuffix(")")
        val normalized = if (unsigned.contains(',') && unsigned.lastIndexOf(',') > unsigned.lastIndexOf('.')) {
            unsigned.replace(".", "").replace(',', '.')
        } else unsigned.replace(",", "")
        val value = normalized.toDoubleOrNull() ?: return null
        return if (negativeByParentheses) -abs(value) else value
    }

    private fun parseDate(raw: String, fallback: LocalDate): String {
        val value = raw.trim().substringBefore('T').substringBefore(' ').take(10)
        val formats = listOf(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        return formats.firstNotNullOfOrNull { format -> runCatching { LocalDate.parse(value, format).toString() }.getOrNull() } ?: fallback.toString()
    }

    private fun normalizeHeader(raw: String): String = Normalizer.normalize(raw.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace('_', ' ')

    private fun stableId(seed: String): Long = abs(seed.hashCode().toLong()).takeIf { it > 0L } ?: 1L

    private val amountPattern = Regex("(?i)(?:r\\$\\s*)?-?\\d{1,3}(?:\\.\\d{3})*,\\d{1,2}|(?:r\\$\\s*)?-?\\d+(?:[.,]\\d{1,2})")
    private val datePattern = Regex("\\b(?:\\d{4}-\\d{2}-\\d{2}|\\d{2}[/-]\\d{2}[/-]\\d{4})\\b")
    private val nubankFullDatePattern = Regex("(?i)\\bFATURA\\s+(\\d{1,2})\\s+(JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)\\s+(\\d{4})\\b")
    private val nubankDatePrefixPattern = Regex("(?i)^\\s*(\\d{1,2})\\s+(JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)(?=\\s|$)")
    private val incomeKeywords = listOf("recebi", "recebido", "receita", "entrada", "salario", "credito", "estorno", "freela")
    private val statementSummaryKeywords = listOf("total da fatura", "total a pagar", "pagamento minimo", "saldo anterior", "limite disponivel")
    private val nubankNonPurchaseKeywords = listOf(
        "estorno", "pagamento", "saldo restante", "juros", "iof", "multa", "encargo",
        "limite total", "limite disponivel", "total de compras", "total a pagar",
        "pagamento minimo", "fatura anterior", "saldo em aberto", "valor maximo",
        "saque no credito", "pix no credito", "pagamentos de boleto", "operacoes de credito",
        "custo efetivo total"
    )
}
