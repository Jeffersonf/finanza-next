package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.TransactionRow
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import java.time.LocalDate
import java.time.YearMonth

data class CategoryUi(val name: String, val amount: String, val share: Float, val color: Color)
data class MonthTrendUi(val label: String, val income: String, val spent: String, val incomeShare: Float, val spentShare: Float)

private fun analysisMoneyValue(raw: String): Double? {
    val clean = raw.replace("R$", "", ignoreCase = true).replace(" ", "").trim()
    if (clean.isBlank()) return null
    val normalized = if (clean.contains(',')) {
        clean.replace(".", "").replace(',', '.')
    } else clean
    return normalized.toDoubleOrNull()
}

private fun analysisMoneyNumber(raw: String): Double = analysisMoneyValue(raw) ?: 0.0

@Composable
fun AnalysisScreen(
    income: String,
    spent: String,
    categories: List<CategoryUi>,
    trends: List<MonthTrendUi>,
    transactions: List<TransactionUi>,
    accounts: List<AccountUi>,
    onTransaction: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var dateScope by remember { mutableStateOf("month") }
    var sort by remember { mutableStateOf("newest") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var accountFilter by remember { mutableStateOf<String?>(null) }
    var minimumAmount by remember { mutableStateOf("") }
    var maximumAmount by remember { mutableStateOf("") }
    var modernFiltersExpanded by remember { mutableStateOf(false) }
    val experience = LocalAppExperience.current
    val finanza = experience.usesFinanzaVisuals
    val web = experience == AppExperience.WEB
    val visibleTransactions = remember(transactions, query, filter, dateScope, sort, categoryFilter, accountFilter, minimumAmount, maximumAmount) {
        val today = LocalDate.now()
        val minimum = analysisMoneyValue(minimumAmount)
        val maximum = analysisMoneyValue(maximumAmount)
        val visible = transactions.filter { item ->
            val matchesType = filter == "all" || (filter == "income" && item.income) || (filter == "expense" && !item.income)
            val normalized = query.trim().lowercase()
            val matchesCategory = categoryFilter == null || item.category == categoryFilter
            val matchesAccount = accountFilter == null || item.accountId == accountFilter
            val matchesDate = runCatching { LocalDate.parse(item.dateKey) }.getOrNull()?.let { date ->
                when (dateScope) {
                    "month" -> YearMonth.from(date) == YearMonth.from(today)
                    "30days" -> !date.isBefore(today.minusDays(29)) && !date.isAfter(today)
                    else -> true
                }
            } ?: dateScope == "all"
            val amount = analysisMoneyNumber(item.amount)
            val matchesAmount = (minimum == null || amount >= minimum) && (maximum == null || amount <= maximum)
            matchesType && matchesDate && matchesCategory && matchesAccount && matchesAmount && (normalized.isBlank() || item.title.lowercase().contains(normalized) || item.category.lowercase().contains(normalized))
        }
        when (sort) {
            "largest" -> visible.sortedByDescending { analysisMoneyNumber(it.amount) }
            "smallest" -> visible.sortedBy { analysisMoneyNumber(it.amount) }
            else -> visible.sortedWith(compareByDescending<TransactionUi> { it.dateKey }.thenByDescending { it.id })
        }
    }
    val transactionsByDate = remember(visibleTransactions) { visibleTransactions.groupBy { it.date } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 108.dp)
    ) {
        item {
            if (finanza) {
                Text(if (web) "Análises" else "Análise", style = if (web) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
                Text(
                    if (web) "Leitura clara do seu dinheiro no mês" else "Entradas, saídas e evolução dos seus gastos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            } else {
                Text("Leitura do mês", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f))
                Text("Análise", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(18.dp))
            if (finanza) {
                Row(Modifier.fillMaxWidth()) {
                    Summary("Entradas", income, Modifier.weight(1f), income = true)
                    Spacer(Modifier.width(10.dp))
                    Summary("Saídas", spent, Modifier.weight(1f), income = false)
                }
            } else {
                ModernAnalysisHero(income, spent)
            }
            if (finanza) {
                Spacer(Modifier.height(10.dp))
                FinanzaAnalysisPulse(categories)
            }
            Text(if (finanza) "Fluxo de caixa" else "Últimos 6 meses", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
            if (finanza) {
                FinanzaCashFlowChart(trends)
            } else ModernCashFlowCard(trends)
            Text("Por categoria", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
        }
        if (finanza) item {
            FinanzaCategoryShareChart(categories)
            Spacer(Modifier.height(10.dp))
            FinanzaCategoryBreakdown(categories)
        }
        else item { ModernCategoryBreakdown(categories) }
        item {
            Text("Movimentos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
            if (finanza) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            if (experience == AppExperience.WEB) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisAmountField("Valor mínimo", minimumAmount, { minimumAmount = it }, Modifier.weight(1f))
                    AnalysisAmountField("Valor máximo", maximumAmount, { maximumAmount = it }, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
                listOf("all" to "Todos", "expense" to "Gastos", "income" to "Receitas").forEach { (id, label) ->
                    AnalysisFilterChip(finanza, filter == id, label) { filter = id }
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)
            ) {
                listOf("month" to "Este mês", "30days" to "30 dias", "all" to "Todo período").forEach { (id, label) ->
                    AnalysisFilterChip(finanza, dateScope == id, label) { dateScope = id }
                }
                listOf("newest" to "Recentes", "largest" to "Maior valor", "smallest" to "Menor valor").forEach { (id, label) ->
                    AnalysisFilterChip(finanza, sort == id, label) { sort = id }
                }
            }
            val transactionCategories = transactions.map { it.category }.filter { it.isNotBlank() }.distinct().take(8)
            val transactionAccountIds = transactions.map { it.accountId }.filter { it.isNotBlank() }.distinct()
            val transactionAccounts = accounts.filter { it.id in transactionAccountIds }
            if (experience == AppExperience.NEXT) {
                ModernFilterToggle(
                    expanded = modernFiltersExpanded,
                    active = minimumAmount.isNotBlank() || maximumAmount.isNotBlank() || categoryFilter != null || accountFilter != null,
                    onClick = { modernFiltersExpanded = !modernFiltersExpanded }
                )
            }
            val showExtendedFilters = experience != AppExperience.NEXT || modernFiltersExpanded
            if (showExtendedFilters && experience == AppExperience.NEXT) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisAmountField("Valor m\u00ednimo", minimumAmount, { minimumAmount = it }, Modifier.weight(1f))
                    AnalysisAmountField("Valor m\u00e1ximo", maximumAmount, { maximumAmount = it }, Modifier.weight(1f))
                }
            }
            if (showExtendedFilters && transactionCategories.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)
                ) {
                    AnalysisFilterChip(finanza, categoryFilter == null, "Categorias") { categoryFilter = null }
                    transactionCategories.forEach { category ->
                        AnalysisFilterChip(finanza, categoryFilter == category, category) { categoryFilter = category }
                    }
                }
            }
            if (showExtendedFilters && transactionAccounts.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)
                ) {
                    AnalysisFilterChip(finanza, accountFilter == null, "Contas") { accountFilter = null }
                    transactionAccounts.forEach { account ->
                        AnalysisFilterChip(finanza, accountFilter == account.id, account.name) { accountFilter = account.id }
                    }
                }
            }
        }
        if (visibleTransactions.isEmpty()) item {
            Text("Nenhum movimento encontrado.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f), modifier = Modifier.padding(vertical = 18.dp))
        }
        transactionsByDate.forEach { (date, groupedTransactions) ->
            item(key = "date-$date") {
                Text(
                    date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 3.dp)
                )
            }
            items(groupedTransactions, key = { it.id }) { item -> TransactionRow(item, { onTransaction(item.id) }) }
        }
    }
}

@Composable
private fun ModernFilterToggle(expanded: Boolean, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (active || expanded) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        modifier = Modifier.padding(top = 2.dp).clip(shape).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (expanded) "Ocultar filtros" else if (active) "Filtros ativos" else "Mais filtros",
                modifier = Modifier.padding(start = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalysisAmountField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    val experience = LocalAppExperience.current
    if (experience == AppExperience.WEB) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    } else {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier.clip(RoundedCornerShape(14.dp)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AnalysisFilterChip(finanza: Boolean, selected: Boolean, label: String, onClick: () -> Unit) {
    if (finanza) {
        FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun FinanzaAnalysisPulse(categories: List<CategoryUi>) {
    val leading = categories.maxByOrNull { it.share }
    val web = LocalAppExperience.current == AppExperience.WEB
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (web) 0.06f else 1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.padding(start = 11.dp).weight(1f)) {
                Text("Panorama do mês", style = MaterialTheme.typography.titleSmall)
                Text(
                    leading?.let { "${it.name} concentra ${(it.share.coerceIn(0f, 1f) * 100).toInt()}% das despesas." }
                        ?: "Registre despesas para acompanhar a distribuição.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinanzaCategoryShareChart(categories: List<CategoryUi>) {
    val visible = categories.filter { it.share > 0f }.take(5)
    val emptyChartColor = MaterialTheme.colorScheme.outlineVariant
    val tokens = LocalAppExperienceTokens.current
    val web = LocalAppExperience.current == AppExperience.WEB
    val chartRadius = if (web) tokens.cardRadius else 18.dp
    Surface(
        shape = RoundedCornerShape(chartRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (web) 0.06f else 1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                    var startAngle = -90f
                    if (visible.isEmpty()) {
                        drawArc(emptyChartColor, 0f, 360f, false, style = stroke)
                    } else {
                        visible.forEach { category ->
                            val sweep = 360f * category.share.coerceIn(0f, 1f)
                            drawArc(category.color, startAngle, sweep, false, style = stroke)
                            startAngle += sweep
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gastos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${visible.size}", style = MaterialTheme.typography.titleLarge)
                    Text("categorias", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text("Distribuição", style = MaterialTheme.typography.titleSmall)
                Text("Peso por categoria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(9.dp))
                if (visible.isEmpty()) {
                    Text("Sem dados suficientes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else visible.take(3).forEach { category ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(category.color))
                        Text(category.name, modifier = Modifier.padding(start = 6.dp).weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text("${(category.share * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(label: String, value: String, modifier: Modifier, income: Boolean) {
    val tokens = LocalAppExperienceTokens.current
    val experience = LocalAppExperience.current
    val finanza = experience.usesFinanzaVisuals
    val web = experience == AppExperience.WEB
    Column(
        modifier
            .clip(RoundedCornerShape(tokens.cardRadius))
            .background(
                when {
                    web -> MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    finanza -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .then(
                if (web) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.06f), RoundedCornerShape(tokens.cardRadius)) else Modifier
            )
            .padding(if (tokens.denseLists) 16.dp else 18.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = if (finanza && income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ModernAnalysisHero(income: String, spent: String) {
    val tokens = LocalAppExperienceTokens.current
    val shape = RoundedCornerShape(tokens.cardRadius + 8.dp)
    val muted = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.62f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Visão do mês", style = MaterialTheme.typography.labelLarge, color = muted)
            Text("Fluxo de caixa", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.inverseOnSurface)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Entradas", style = MaterialTheme.typography.labelSmall, color = muted)
                    Text(income, style = MaterialTheme.typography.titleMedium, color = Color(0xFF34C759))
                }
                Box(
                    Modifier.width(1.dp).height(38.dp)
                        .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.13f))
                )
                Column(Modifier.weight(1f).padding(start = 18.dp)) {
                    Text("Saídas", style = MaterialTheme.typography.labelSmall, color = muted)
                    Text(spent, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FinanzaCashFlowChart(trends: List<MonthTrendUi>) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val tokens = LocalAppExperienceTokens.current
    val web = LocalAppExperience.current == AppExperience.WEB
    val chartRadius = if (web) tokens.cardRadius else 20.dp
    Surface(
        shape = RoundedCornerShape(chartRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (web) 0.06f else 1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Receitas vs despesas", style = MaterialTheme.typography.titleSmall)
                    Text("Últimos 6 meses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LegendDot(incomeColor, "Entradas")
                Spacer(Modifier.width(8.dp))
                LegendDot(expenseColor, "Saídas")
            }
            Canvas(Modifier.fillMaxWidth().height(146.dp).padding(top = 14.dp)) {
                val count = trends.size.coerceAtLeast(1)
                val groupWidth = size.width / count
                val baseline = size.height - 5.dp.toPx()
                val chartHeight = baseline - 10.dp.toPx()
                trends.forEachIndexed { index, trend ->
                    val center = groupWidth * (index + 0.5f)
                    val barWidth = (groupWidth * 0.23f).coerceAtMost(18.dp.toPx())
                    val incomeHeight = chartHeight * trend.incomeShare.coerceIn(0f, 1f)
                    val expenseHeight = chartHeight * trend.spentShare.coerceIn(0f, 1f)
                    drawRoundRect(
                        incomeColor,
                        topLeft = Offset(center - barWidth - 2.dp.toPx(), baseline - incomeHeight),
                        size = Size(barWidth, incomeHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        expenseColor,
                        topLeft = Offset(center + 2.dp.toPx(), baseline - expenseHeight),
                        size = Size(barWidth, expenseHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                trends.forEach { trend ->
                    Text(
                        trend.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernCashFlowCard(trends: List<MonthTrendUi>) {
    val active = trends.filter { it.incomeShare > 0f || it.spentShare > 0f }
    val tokens = LocalAppExperienceTokens.current
    val expenseColor = MaterialTheme.colorScheme.error
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        shape = RoundedCornerShape(tokens.cardRadius),
        color = if (darkSurface) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f) else MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Entradas e saídas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (active.size < 2) "O histórico aparece conforme os próximos meses forem registrados." else "Comparativo dos meses com movimentação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                LegendDot(Color(0xFF34C759), "Entradas")
                Spacer(Modifier.width(8.dp))
                LegendDot(expenseColor, "Saídas")
            }
            if (active.isEmpty()) {
                Text("Ainda não há movimentações para analisar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 28.dp))
            } else {
                Canvas(Modifier.fillMaxWidth().height(128.dp).padding(top = 16.dp)) {
                    val groupWidth = size.width / active.size
                    val baseline = size.height - 3.dp.toPx()
                    val chartHeight = baseline - 8.dp.toPx()
                    active.forEachIndexed { index, trend ->
                        val center = groupWidth * (index + 0.5f)
                        val barWidth = (groupWidth * 0.24f).coerceIn(14.dp.toPx(), 30.dp.toPx())
                        val incomeHeight = chartHeight * trend.incomeShare.coerceIn(0f, 1f)
                        val expenseHeight = chartHeight * trend.spentShare.coerceIn(0f, 1f)
                        drawRoundRect(Color(0xFF34C759), Offset(center - barWidth - 3.dp.toPx(), baseline - incomeHeight), Size(barWidth, incomeHeight.coerceAtLeast(3.dp.toPx())), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()))
                        drawRoundRect(expenseColor, Offset(center + 3.dp.toPx(), baseline - expenseHeight), Size(barWidth, expenseHeight.coerceAtLeast(3.dp.toPx())), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    active.forEach { trend ->
                        Text(trend.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FinanzaCategoryBreakdown(categories: List<CategoryUi>) {
    val visible = categories.filter { it.share > 0f }.take(5)
    val tokens = LocalAppExperienceTokens.current
    val web = LocalAppExperience.current == AppExperience.WEB
    val chartRadius = if (web) tokens.cardRadius else 18.dp
    Surface(
        shape = RoundedCornerShape(chartRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (web) 0.06f else 1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Distribuição dos gastos", style = MaterialTheme.typography.titleSmall)
            Text("Categorias com maior impacto no mês", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            if (visible.isEmpty()) {
                Text("Sem gastos categorizados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else visible.forEach { category ->
                Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(category.color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(category.color))
                        }
                        Text(category.name, modifier = Modifier.padding(start = 9.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(category.amount, style = MaterialTheme.typography.labelLarge)
                            Text("${(category.share.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))) {
                        Box(Modifier.fillMaxWidth(category.share.coerceIn(0f, 1f)).height(5.dp).background(category.color))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernCategoryBreakdown(categories: List<CategoryUi>) {
    val visible = categories.filter { it.share > 0f }.take(6)
    val tokens = LocalAppExperienceTokens.current
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        shape = RoundedCornerShape(tokens.cardRadius),
        color = if (darkSurface) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (visible.isEmpty()) {
                Text("Sem gastos categorizados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            } else visible.forEachIndexed { index, category ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(category.color.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(category.color))
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(category.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${(category.share.coerceIn(0f, 1f) * 100).toInt()}% das despesas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(category.amount, style = MaterialTheme.typography.titleSmall)
                }
                if (index < visible.lastIndex) {
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (darkSurface) 0.16f else 0.44f))
                    )
                }
            }
        }
    }
}
