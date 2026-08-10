package com.finanza.next.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.TransactionRow
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import kotlin.math.abs

private enum class HistoryType(val label: String) {
    ALL("Todos"),
    EXPENSE("Gastos"),
    INCOME("Entradas")
}

private enum class HistorySort(val label: String) {
    DATE_DESC("Mais recentes"),
    DATE_ASC("Mais antigas"),
    AMOUNT_DESC("Maior valor"),
    AMOUNT_ASC("Menor valor")
}

private enum class HistoryPeriod(val label: String) {
    DAYS_7("7 dias"),
    MONTH_1("1 mês"),
    MONTHS_3("3 meses"),
    MONTHS_6("6 meses"),
    ALL("Tudo")
}

@Composable
fun TransactionHistoryScreen(
    transactions: List<TransactionUi>,
    accounts: List<AccountUi> = emptyList(),
    onTransaction: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(HistoryType.ALL) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var accountFilter by remember { mutableStateOf<String?>(null) }
    var minimumAmount by remember { mutableStateOf("") }
    var maximumAmount by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(HistorySort.DATE_DESC) }
    var period by remember { mutableStateOf(HistoryPeriod.MONTH_1) }
    val web = LocalAppExperience.current == AppExperience.WEB
    val normalizedQuery = query.trim().lowercase()
    val categories = remember(transactions) { transactions.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
    val transactionAccounts = remember(transactions, accounts) {
        val names = accounts.associateBy { it.id }
        transactions.mapNotNull { item ->
            item.accountId.takeIf { it.isNotBlank() }?.let { id -> id to (names[id]?.name ?: id) }
        }.distinctBy { it.first }.sortedBy { it.second }
    }
    val minValue = remember(minimumAmount) { historyMoney(minimumAmount) }
    val maxValue = remember(maximumAmount) { historyMoney(maximumAmount) }
    val filtered = remember(transactions, normalizedQuery, type, categoryFilter, accountFilter, minValue, maxValue, sort, period) {
        transactions.filter { item ->
            val amount = historyMoney(item.amount)
            val date = historyDate(item.dateKey, item.date)
            val today = java.time.LocalDate.now()
            val periodStart = when (period) {
                HistoryPeriod.DAYS_7 -> today.minusDays(6)
                HistoryPeriod.MONTH_1 -> today.withDayOfMonth(1)
                HistoryPeriod.MONTHS_3 -> today.minusMonths(2).withDayOfMonth(1)
                HistoryPeriod.MONTHS_6 -> today.minusMonths(5).withDayOfMonth(1)
                HistoryPeriod.ALL -> java.time.LocalDate.MIN
            }
            (type == HistoryType.ALL || (type == HistoryType.INCOME) == item.income) &&
                (period == HistoryPeriod.ALL || (!date.isBefore(periodStart) && !date.isAfter(today))) &&
                (categoryFilter == null || item.category == categoryFilter) &&
                (accountFilter == null || item.accountId == accountFilter) &&
                (minValue?.let { amount != null && amount >= it } ?: true) &&
                (maxValue?.let { amount != null && amount <= it } ?: true) &&
                (normalizedQuery.isBlank() || item.title.lowercase().contains(normalizedQuery) || item.category.lowercase().contains(normalizedQuery))
        }.let { items ->
            when (sort) {
                HistorySort.DATE_DESC -> items.sortedByDescending { historyDate(it.dateKey, it.date) }
                HistorySort.DATE_ASC -> items.sortedBy { historyDate(it.dateKey, it.date) }
                HistorySort.AMOUNT_DESC -> items.sortedByDescending { historyMoney(it.amount) ?: 0.0 }
                HistorySort.AMOUNT_ASC -> items.sortedBy { historyMoney(it.amount) ?: 0.0 }
            }
        }
    }
    val groups = remember(filtered) { filtered.groupBy { historyMonthLabel(it.dateKey, it.date) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("Histórico", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${filtered.size} lançamento${if (filtered.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                    )
                }
                IconButton(onClick = onQuickAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "Adicionar lançamento", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Buscar lançamento ou categoria") },
                shape = if (web) MaterialTheme.shapes.medium else MaterialTheme.shapes.large
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryType.entries.forEach { option ->
                    FilterChip(
                        selected = option == type,
                        onClick = { type = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }
        if (web) {
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryPeriod.entries.forEach { option ->
                        FilterChip(
                            selected = option == period,
                            onClick = { period = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
        }
        if (web) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minimumAmount,
                        onValueChange = { minimumAmount = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text("Valor mínimo") },
                        placeholder = { Text("R$ 0,00") },
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = maximumAmount,
                        onValueChange = { maximumAmount = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text("Valor máximo") },
                        placeholder = { Text("R$ 0,00") },
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistorySort.entries.forEach { option ->
                        FilterChip(
                            selected = option == sort,
                            onClick = { sort = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
        }
        if (categories.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text("Categorias") }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = categoryFilter == category,
                            onClick = { categoryFilter = if (categoryFilter == category) null else category },
                            label = { Text(category) }
                        )
                    }
                }
            }
        }
        if (transactionAccounts.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = accountFilter == null,
                        onClick = { accountFilter = null },
                        label = { Text("Contas") }
                    )
                    transactionAccounts.forEach { (id, name) ->
                        FilterChip(
                            selected = accountFilter == id,
                            onClick = { accountFilter = if (accountFilter == id) null else id },
                            label = { Text(name) }
                        )
                    }
                }
            }
        }
        if (groups.isEmpty()) {
            item {
                Spacer(Modifier.height(22.dp))
                Text(
                    "Nenhum lançamento encontrado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            groups.forEach { (month, items) ->
                item(key = "header-$month") {
                    Text(month, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                items(items.size, key = { index -> items[index].id }) { index ->
                    TransactionRow(items[index], { onTransaction(items[index].id) })
                }
            }
        }
    }
}

private fun historyMonthLabel(dateKey: String, fallback: String): String {
    val date = dateKey.ifBlank { fallback }
    val parts = date.split('/')
    if (parts.size >= 3) return "${parts[1]}/${parts[2]}"
    return runCatching {
        val iso = java.time.LocalDate.parse(date)
        "%02d/%d".format(iso.monthValue, iso.year)
    }.getOrDefault(fallback)
}

private fun historyMoney(raw: String): Double? {
    val normalized = raw
        .replace("R$", "", ignoreCase = true)
        .replace("\\s".toRegex(), "")
        .trim()
    if (normalized.isBlank()) return null
    return normalized
        .let { value ->
            if (value.contains(',') && value.contains('.')) value.replace(".", "").replace(',', '.')
            else value.replace(',', '.')
        }
        .toDoubleOrNull()
        ?.let(::abs)
}

private fun historyDate(dateKey: String, fallback: String): java.time.LocalDate {
    val raw = dateKey.ifBlank { fallback }
    return runCatching {
        if (raw.contains('/')) {
            val parts = raw.split('/')
            java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } else java.time.LocalDate.parse(raw)
    }.getOrElse { java.time.LocalDate.MIN }
}
