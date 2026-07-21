package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.TransactionRow
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

data class CategoryUi(val name: String, val amount: String, val share: Float, val color: Color)
data class MonthTrendUi(val label: String, val income: String, val spent: String, val incomeShare: Float, val spentShare: Float)

@Composable
fun AnalysisScreen(income: String, spent: String, categories: List<CategoryUi>, trends: List<MonthTrendUi>, transactions: List<TransactionUi>, onTransaction: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    val visibleTransactions = remember(transactions, query, filter) {
        transactions.filter { item ->
            val matchesType = filter == "all" || (filter == "income" && item.income) || (filter == "expense" && !item.income)
            val normalized = query.trim().lowercase()
            matchesType && (normalized.isBlank() || item.title.lowercase().contains(normalized) || item.category.lowercase().contains(normalized))
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 108.dp)
    ) {
        item {
            if (finanza) {
                Text("Análise", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Entradas, saídas e evolução dos seus gastos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            } else {
                Text("Leitura do mês", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f))
                Text("Análise", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                Summary("Entradas", income, Modifier.weight(1f), income = true)
                Spacer(Modifier.width(10.dp))
                Summary("Saídas", spent, Modifier.weight(1f), income = false)
            }
            Text(if (finanza) "Fluxo de caixa" else "Últimos 6 meses", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
            if (finanza) {
                FinanzaCashFlowChart(trends)
            } else ModernCashFlowCard(trends)
            Text("Por categoria", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
        }
        if (finanza) item { FinanzaCategoryBreakdown(categories) }
        else item { ModernCategoryBreakdown(categories) }
        item {
            Text("Movimentos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
                listOf("all" to "Todos", "expense" to "Gastos", "income" to "Receitas").forEach { (id, label) ->
                    FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label) })
                }
            }
        }
        if (visibleTransactions.isEmpty()) item {
            Text("Nenhum movimento encontrado.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f), modifier = Modifier.padding(vertical = 18.dp))
        }
        items(visibleTransactions, key = { it.id }) { item -> TransactionRow(item, { onTransaction(item.id) }) }
    }
}

@Composable
private fun Summary(label: String, value: String, modifier: Modifier, income: Boolean) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    Column(
        modifier
            .clip(RoundedCornerShape(tokens.cardRadius))
            .background(if (finanza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
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
private fun FinanzaCashFlowChart(trends: List<MonthTrendUi>) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
    Surface(
        shape = RoundedCornerShape(tokens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f))
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
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
    Surface(
        shape = RoundedCornerShape(tokens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f))
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
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)))
                }
            }
        }
    }
}
