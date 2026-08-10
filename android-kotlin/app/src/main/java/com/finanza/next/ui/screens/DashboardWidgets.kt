@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.finanza.next.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.features.FeatureItemUi
import com.finanza.next.FinanzaPreferences
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.BillRow
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.HeroBalanceCard
import com.finanza.next.ui.components.TransactionRow
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import org.json.JSONArray
import org.json.JSONObject

private data class DashboardWidgetDef(
    val id: String,
    val title: String,
    val description: String,
    val group: String,
    val icon: ImageVector,
    val default: Boolean,
    val pinned: Boolean = false
)

private data class DashboardWidgetConfig(val active: Set<String>, val order: List<String>)

private data class OverviewMosaicItem(
    val title: String,
    val detail: String,
    val amount: Double,
    val icon: ImageVector,
    val colorIndex: Int
)

private val dashboardWidgetDefs = listOf(
    DashboardWidgetDef("workbench", "Atalhos do Finanza", "Entrada para as áreas principais", "Apoio", Icons.Rounded.SpaceDashboard, true, true),
    DashboardWidgetDef("cards", "Resumo do dia a dia", "Saldo, entradas e gastos", "Essencial", Icons.Rounded.Analytics, true),
    DashboardWidgetDef("dailyweek", "Ritmo semanal", "Entradas, gastos e saldo da semana", "Essencial", Icons.Rounded.CalendarMonth, false),
    DashboardWidgetDef("dailymonth", "Ritmo mensal", "Entradas, gastos e saldo do mês", "Essencial", Icons.Rounded.CalendarMonth, false),
    DashboardWidgetDef("dailyyear", "Ritmo anual", "Entradas, gastos e saldo do ano", "Essencial", Icons.Rounded.CalendarMonth, false),
    DashboardWidgetDef("cashflow", "Fluxo do mês", "Entradas, saídas e saldo dos lançamentos", "Análise", Icons.AutoMirrored.Rounded.ShowChart, false),
    DashboardWidgetDef("charts", "Gráficos", "Fluxo de caixa e categorias", "Análise", Icons.Rounded.BarChart, false),
    DashboardWidgetDef("commitments", "Próximos vencimentos", "Contas e compromissos", "Essencial", Icons.AutoMirrored.Rounded.ReceiptLong, true),
    DashboardWidgetDef("renewals", "Renovações próximas", "Assinaturas, dívidas e contratos", "Apoio", Icons.Rounded.Subscriptions, false),
    DashboardWidgetDef("overview", "Visão geral", "Maiores movimentos e compromissos", "Essencial", Icons.Rounded.GridView, true),
    DashboardWidgetDef("subscriptions", "Assinaturas", "Recorrentes por peso mensal", "Apoio", Icons.Rounded.Subscriptions, false),
    DashboardWidgetDef("quickactions", "Ações rápidas", "Menos toques para lançar", "Essencial", Icons.Rounded.Bolt, true),
    DashboardWidgetDef("recent", "Últimas transações", "Lançamentos recentes", "Essencial", Icons.Rounded.History, true),
    DashboardWidgetDef("accounts", "Saldos das contas", "Saldo de cada conta", "Apoio", Icons.Rounded.AccountBalanceWallet, false),
    DashboardWidgetDef("budgets", "Orçamentos rápidos", "Uso por categoria", "Apoio", Icons.Rounded.TrackChanges, false),
    DashboardWidgetDef("goals", "Metas rápidas", "Progresso das metas", "Apoio", Icons.Rounded.Flag, false),
    DashboardWidgetDef("shopping", "Lista de compras", "Itens pendentes da lista ativa", "Apoio", Icons.Rounded.History, false),
    DashboardWidgetDef("vehicles", "Veículos", "Garagem e manutenção", "Apoio", Icons.Rounded.AccountBalanceWallet, false),
    DashboardWidgetDef("barcats", "Ranking de gastos", "Categorias com maior peso", "Análise", Icons.Rounded.BarChart, false),
    DashboardWidgetDef("compare", "Comparar meses", "Variação entre os dois últimos meses", "Análise", Icons.Rounded.Analytics, false),
    DashboardWidgetDef("ministats", "Mini estatísticas", "Média diária, maior gasto e dias", "Análise", Icons.Rounded.Analytics, false),
    DashboardWidgetDef("saverate", "Taxa de economia", "Quanto sobra das receitas", "Análise", Icons.AutoMirrored.Rounded.ShowChart, false),
    DashboardWidgetDef("projection", "Projeção do mês", "Estimativa de gastos até o fechamento", "Análise", Icons.AutoMirrored.Rounded.ShowChart, false),
    DashboardWidgetDef("weekly", "Pulso semanal", "Média e total dos últimos sete dias", "Análise", Icons.Rounded.CalendarMonth, false),
    DashboardWidgetDef("anomaly", "Maior lançamento", "Movimento que mais pesou no período", "Análise", Icons.Rounded.ArrowDownward, false),
    DashboardWidgetDef("budalerts", "Alertas de orçamento", "Categorias perto do limite", "Apoio", Icons.Rounded.TrackChanges, false)
)

@Composable
fun DashboardWidgets(
    period: String,
    balance: String,
    income: String,
    spent: String,
    transactions: List<TransactionUi>,
    accounts: List<AccountUi>,
    bills: List<BillUi>,
    categories: List<CategoryUi>,
    trends: List<MonthTrendUi>,
    features: FeatureCenterUiState,
    onAdd: () -> Unit,
    onAllTransactions: () -> Unit,
    onTransaction: (Long) -> Unit,
    onBill: (Long) -> Unit,
    onAccounts: () -> Unit,
    onAnalysis: () -> Unit,
    onFeatures: () -> Unit,
    onSettings: () -> Unit,
    hiddenWidgetIds: Set<String> = emptySet(),
    showManagerHeader: Boolean = true,
    managerVisible: Boolean? = null,
    onManagerVisibleChange: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val experience = LocalAppExperience.current
    val finanza = experience.usesFinanzaVisuals
    var config by remember(experience) { mutableStateOf(loadDashboardWidgetConfig(context, experience)) }
    var localEditing by remember { mutableStateOf(false) }
    val editing = managerVisible ?: localEditing

    fun setEditing(visible: Boolean) {
        if (onManagerVisibleChange != null) onManagerVisibleChange(visible) else localEditing = visible
    }

    fun update(next: DashboardWidgetConfig) {
        val normalized = normalizeDashboardWidgetConfig(next)
        config = normalized
        saveDashboardWidgetConfig(context, experience, normalized)
    }

    if (showManagerHeader) {
        DashboardManagerHeader(config.active.size, dashboardWidgetDefs.size) { setEditing(true) }
        Spacer(Modifier.height(10.dp))
    }
    val orderedWidgetIds = config.order
        .filter(config.active::contains)
        .filterNot(hiddenWidgetIds::contains)
        // The Modern workbench already contains the fast actions. Rendering both
        // blocks makes the dashboard repeat the same commands and adds weight.
        .filterNot { id -> experience == AppExperience.NEXT && id == "quickactions" }
        .let { ids ->
            if (!finanza || "workbench" !in ids) ids
            else ids.filterNot { it == "workbench" } + "workbench"
        }
    orderedWidgetIds.forEach { id ->
        when (id) {
            "workbench" -> DashboardWorkbench(onAccounts, onAnalysis, onFeatures, onSettings, onAllTransactions)
            "cards" -> if (finanza) {
                FinanzaSummaryGrid(balance, income, spent, bills)
            } else {
                HeroBalanceCard("Disponível", period, balance, income, spent)
            }
            "dailyweek" -> DashboardPeriodRhythm("Ritmo semanal", "Últimos sete dias", transactions, RhythmPeriod.WEEK, onAnalysis)
            "dailymonth" -> DashboardPeriodRhythm("Ritmo mensal", "Mês atual", transactions, RhythmPeriod.MONTH, onAnalysis)
            "dailyyear" -> DashboardPeriodRhythm("Ritmo anual", "Ano atual", transactions, RhythmPeriod.YEAR, onAnalysis)
            "cashflow" -> DashboardCashFlow(transactions, onAnalysis)
            "charts" -> DashboardChartsOverview(transactions, categories, onAnalysis)
            "commitments" -> DashboardBills(bills, onBill, onAccounts)
            "renewals" -> DashboardRenewals(features, onFeatures)
            "overview" -> DashboardOverviewMosaic(transactions, bills, features, income, spent, balance, onFeatures)
            "subscriptions" -> DashboardSubscriptions(features, onFeatures)
            "quickactions" -> DashboardQuickActions(onAdd, onAllTransactions, onAccounts)
            "recent" -> DashboardRecent(transactions, onTransaction, onAllTransactions)
            "accounts" -> DashboardAccounts(accounts, onAccounts)
            "budgets" -> DashboardFeatureList("Orçamentos", features, "budgets", MaterialTheme.colorScheme.primary, onFeatures)
            "goals" -> DashboardFeatureList("Metas", features, "goals", MaterialTheme.colorScheme.tertiary, onFeatures)
            "shopping" -> DashboardFeatureList("Lista de compras", features, "shopping", MaterialTheme.colorScheme.primary, onFeatures)
            "vehicles" -> DashboardFeatureList("Veículos", features, "vehicles", MaterialTheme.colorScheme.secondary, onFeatures)
            "barcats" -> DashboardCategories(categories, onAnalysis)
            "compare" -> DashboardMonthCompare(trends, onAnalysis)
            "ministats" -> DashboardMiniStats(transactions, onAllTransactions)
            "saverate" -> DashboardSavingsRate(transactions, onAnalysis)
            "projection" -> DashboardProjection(transactions, onAnalysis)
            "weekly" -> DashboardWeeklyPulse(transactions, onAnalysis)
            "anomaly" -> DashboardLargestExpense(transactions, onAllTransactions)
            "budalerts" -> DashboardBudgetAlerts(features, onFeatures)
        }
        Spacer(Modifier.height(10.dp))
    }

    if (editing) {
        DashboardWidgetManager(
            config = config,
            experience = experience,
            onDismiss = { setEditing(false) },
            onUpdate = ::update
        )
    }
}

@Composable
private fun DashboardCashFlow(transactions: List<TransactionUi>, onAnalysis: () -> Unit) {
    val income = transactions.filter { it.income }.sumOf { moneyNumber(it.amount) }
    val spent = transactions.filterNot { it.income }.sumOf { moneyNumber(it.amount) }
    val total = (income + spent).coerceAtLeast(1.0)
    val incomeShare = (income / total).toFloat().coerceIn(0f, 1f)
    val balance = income - spent
    WidgetSurface("Fluxo do mês", "Leitura dos lançamentos visíveis", Icons.AutoMirrored.Rounded.ShowChart, onAnalysis) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowMetric("Entradas", moneyLabel(income), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FlowMetric("Saídas", moneyLabel(spent), MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 13.dp).height(8.dp)
                .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier.fillMaxWidth(incomeShare).height(8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            if (balance >= 0.0) "Saldo positivo de ${moneyLabel(balance)}" else "Saldo negativo de ${moneyLabel(-balance)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 9.dp)
        )
    }
}

private enum class RhythmPeriod { WEEK, MONTH, YEAR }

@Composable
private fun DashboardPeriodRhythm(
    title: String,
    subtitle: String,
    transactions: List<TransactionUi>,
    period: RhythmPeriod,
    onAnalysis: () -> Unit
) {
    val today = LocalDate.now()
    val visible = transactions.filter { item ->
        val date = runCatching { LocalDate.parse(item.dateKey) }.getOrNull() ?: return@filter false
        when (period) {
            RhythmPeriod.WEEK -> date in today.minusDays(6)..today
            RhythmPeriod.MONTH -> YearMonth.from(date) == YearMonth.from(today)
            RhythmPeriod.YEAR -> date.year == today.year
        }
    }
    val income = visible.filter { it.income }.sumOf { moneyNumber(it.amount) }
    val expenses = visible.filterNot { it.income }.sumOf { moneyNumber(it.amount) }
    val balance = income - expenses
    val days = when (period) {
        RhythmPeriod.WEEK -> 7
        RhythmPeriod.MONTH -> today.dayOfMonth
        RhythmPeriod.YEAR -> today.dayOfYear
    }.coerceAtLeast(1)
    val previousWeekExpenses = if (period == RhythmPeriod.WEEK) {
        val start = today.minusDays(13)
        val end = today.minusDays(7)
        transactions.filterNot { it.income }.filter { item ->
            runCatching { LocalDate.parse(item.dateKey) in start..end }.getOrDefault(false)
        }.sumOf { moneyNumber(it.amount) }
    } else 0.0
    val expenseAverage = expenses / days
    val incomeAverage = income / days
    WidgetSurface(title, subtitle, Icons.Rounded.CalendarMonth, onAnalysis) {
        when (period) {
            RhythmPeriod.WEEK -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowMetric("Gasto da semana", moneyLabel(expenses), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    FlowMetric("Média diária", moneyLabel(expenseAverage), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowMetric("Semana anterior", moneyLabel(previousWeekExpenses), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                    FlowMetric("Entradas", moneyLabel(income), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
            RhythmPeriod.MONTH -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowMetric("Ganho por dia", moneyLabel(incomeAverage), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    FlowMetric("Gasto por dia", moneyLabel(expenseAverage), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowMetric("Gasto no mês", moneyLabel(expenses), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    FlowMetric("Saldo do mês", moneyLabel(balance), if (balance >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
            }
            RhythmPeriod.YEAR -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowMetric("Ganho até hoje", moneyLabel(income), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    FlowMetric("Ganho por dia", moneyLabel(incomeAverage), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowMetric("Gasto anual", moneyLabel(expenses), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    FlowMetric("Gasto por dia", moneyLabel(expenseAverage), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
            }
        }
        Text(
            "${visible.size} lançamento(s) considerados no período.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun DashboardChartsOverview(
    transactions: List<TransactionUi>,
    categories: List<CategoryUi>,
    onAnalysis: () -> Unit
) {
    val expenses = transactions.filterNot { it.income }
    val total = expenses.sumOf { moneyNumber(it.amount) }
    val categoryTotal = categories.sumOf { moneyNumber(it.amount) }
    val topCategory = categories.maxByOrNull { moneyNumber(it.amount) }
    val classifiedShare = if (total > 0.0) (categoryTotal / total * 100.0).coerceIn(0.0, 100.0) else 0.0
    WidgetSurface("Gráficos", "Fluxo de caixa e distribuição por categoria", Icons.Rounded.BarChart, onAnalysis) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowMetric("Gastos", moneyLabel(total), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            FlowMetric("Categorias", categories.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Text(
            topCategory?.let { "Maior categoria: ${it.name} (${it.amount})." }
                ?: "Ainda não há despesas por categoria para desenhar os gráficos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (total > 0.0) {
            Box(
                Modifier.fillMaxWidth().padding(top = 10.dp).height(7.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier.fillMaxWidth(classifiedShare.toFloat() / 100f).height(7.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun DashboardMonthCompare(trends: List<MonthTrendUi>, onAnalysis: () -> Unit) {
    val pair = trends.takeLast(2)
    WidgetSurface("Comparar meses", "Despesas no mês atual versus o anterior", Icons.Rounded.Analytics, onAnalysis) {
        if (pair.size < 2) {
            Text("Registre movimentos em dois meses para comparar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val previous = moneyNumber(pair[0].spent)
            val current = moneyNumber(pair[1].spent)
            val difference = current - previous
            val percent = if (previous > 0.0) (difference / previous * 100.0) else null
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowMetric(pair[0].label, pair[0].spent, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                FlowMetric(pair[1].label, pair[1].spent, if (difference <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
            Text(
                when {
                    difference == 0.0 -> "Mesmo nível de gastos do mês anterior."
                    difference < 0.0 -> "${moneyLabel(-difference)} a menos${percent?.let { " (${String.format(Locale.forLanguageTag("pt-BR"), "%.0f", -it)}%)" }.orEmpty()}."
                    else -> "${moneyLabel(difference)} a mais${percent?.let { " (${String.format(Locale.forLanguageTag("pt-BR"), "%.0f", it)}%)" }.orEmpty()}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun DashboardProjection(transactions: List<TransactionUi>, onAnalysis: () -> Unit) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val spent = transactions.filterNot { it.income }.filter { item ->
        runCatching { YearMonth.from(LocalDate.parse(item.dateKey)) == month }.getOrDefault(false)
    }.sumOf { moneyNumber(it.amount) }
    val projected = if (today.dayOfMonth > 0) spent / today.dayOfMonth * month.lengthOfMonth() else 0.0
    WidgetSurface("Projeção do mês", "Estimativa baseada no ritmo de gastos", Icons.AutoMirrored.Rounded.ShowChart, onAnalysis) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowMetric("Até hoje", moneyLabel(spent), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            FlowMetric("Fechamento", moneyLabel(projected), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Text(
            "Média de ${moneyLabel(spent / today.dayOfMonth.coerceAtLeast(1))} por dia em ${month.lengthOfMonth()} dias.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun DashboardWeeklyPulse(transactions: List<TransactionUi>, onAnalysis: () -> Unit) {
    val today = LocalDate.now()
    val start = today.minusDays(6)
    val expenses = transactions.filterNot { it.income }.filter { item ->
        runCatching { LocalDate.parse(item.dateKey) in start..today }.getOrDefault(false)
    }
    val total = expenses.sumOf { moneyNumber(it.amount) }
    val activeDays = expenses.map { it.dateKey }.distinct().size
    WidgetSurface("Pulso semanal", "Ritmo dos últimos sete dias", Icons.Rounded.CalendarMonth, onAnalysis) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowMetric("Semana", moneyLabel(total), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FlowMetric("Por dia", moneyLabel(total / 7.0), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
        Text(
            if (activeDays == 0) "Nenhuma despesa registrada nos últimos sete dias." else "$activeDays dia(s) com movimentos registrados.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun DashboardLargestExpense(transactions: List<TransactionUi>, onAll: () -> Unit) {
    val largest = transactions.filterNot { it.income }.maxByOrNull { moneyNumber(it.amount) }
    WidgetSurface("Maior lançamento", "Movimento que mais pesou no período", Icons.Rounded.ArrowDownward, onAll) {
        if (largest == null) {
            Text("Nenhuma despesa disponível para analisar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(largest.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("${largest.category} · ${largest.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            Text(largest.amount, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun DashboardBudgetAlerts(features: FeatureCenterUiState, onFeatures: () -> Unit) {
    val alerts = features.modules.firstOrNull { it.id == "budgets" }?.items.orEmpty()
        .filter { (it.progress ?: 0f) >= 0.8f }
        .sortedByDescending { it.progress ?: 0f }
    WidgetSurface("Alertas de orçamento", "Categorias próximas do limite", Icons.Rounded.TrackChanges, onFeatures) {
        if (alerts.isEmpty()) {
            Text("Nenhuma categoria está próxima do limite.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            alerts.take(3).forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 9.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(item.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${((item.progress ?: 0f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = if ((item.progress ?: 0f) >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun FlowMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.11f)).padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
    }
}

@Composable
private fun FinanzaSummaryGrid(
    balance: String,
    income: String,
    spent: String,
    bills: List<BillUi>
) {
    val dueTotal = moneyLabel(bills.sumOf { moneyNumber(it.amount) })
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FinanzaSummaryCard(
                "Saldo total",
                balance,
                "todas as contas",
                Icons.Rounded.AccountBalanceWallet,
                MaterialTheme.colorScheme.tertiary,
                Modifier.weight(1f)
            )
            FinanzaSummaryCard(
                "Receitas",
                income,
                "este mês",
                Icons.Rounded.ArrowUpward,
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FinanzaSummaryCard(
                "Despesas",
                spent,
                "este mês",
                Icons.Rounded.ArrowDownward,
                MaterialTheme.colorScheme.error,
                Modifier.weight(1f)
            )
            FinanzaSummaryCard(
                "A pagar",
                dueTotal,
                "próximos vencimentos",
                Icons.Rounded.CalendarMonth,
                MaterialTheme.colorScheme.secondary,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FinanzaSummaryCard(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier
) {
    val web = LocalAppExperience.current == AppExperience.WEB
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (web) 0.06f else 1f)),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.13f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(6.dp).size(17.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DashboardOverviewMosaic(
    transactions: List<TransactionUi>,
    bills: List<BillUi>,
    features: FeatureCenterUiState,
    income: String,
    spent: String,
    balance: String,
    onAll: () -> Unit
) {
    val finanza = LocalAppExperience.current.usesFinanzaVisuals
    val modern = LocalAppExperience.current == AppExperience.NEXT
    val web = LocalAppExperience.current == AppExperience.WEB
    val featureItems = features.modules
        .filter { it.id in setOf("subscriptions", "debts", "contracts") }
        .flatMap { module ->
            module.items
                .filter { item -> item.status.isBlank() || item.status.equals("active", ignoreCase = true) || item.status.equals("open", ignoreCase = true) }
                .map { item ->
                    val icon = when (module.id) {
                        "subscriptions" -> Icons.Rounded.Subscriptions
                        "debts" -> Icons.Rounded.AccountBalanceWallet
                        else -> Icons.AutoMirrored.Rounded.ReceiptLong
                    }
                    OverviewMosaicItem(item.title, module.title, moneyNumber(item.value), icon, 3)
                }
        }
    val movements = transactions
        .filterNot { it.income }
        .map { OverviewMosaicItem(it.title, it.category, moneyNumber(it.amount), Icons.AutoMirrored.Rounded.ReceiptLong, 0) }
    val dueItems = bills.map { OverviewMosaicItem(it.name, "Vence ${it.due}", moneyNumber(it.amount), Icons.AutoMirrored.Rounded.ReceiptLong, 2) }
    val items = (movements + dueItems + featureItems)
        .filter { it.amount > 0.0 }
        .sortedByDescending { it.amount }
        .distinctBy { "${it.title}|${it.detail}|${it.amount}" }
        .take(8)
    val total = items.sumOf { it.amount }

    WidgetSurface("Visão geral", "O que mais pesa no seu dinheiro", Icons.Rounded.GridView, onAll) {
        if (items.isEmpty()) {
            WidgetEmpty("Adicione gastos, vencimentos ou compromissos para montar sua visao geral.")
        } else {
            val firstRowHeight = if (LocalAppExperienceTokens.current.denseLists) 132.dp else 148.dp
            val secondRowHeight = if (LocalAppExperienceTokens.current.denseLists) 108.dp else 120.dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewTile(items[0], total, 0, Modifier.weight(1.08f).height(firstRowHeight), large = true)
                Column(Modifier.weight(0.92f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.getOrNull(1)?.let { OverviewTile(it, total, 1, Modifier.fillMaxWidth().height((firstRowHeight - 8.dp) / 2)) }
                    items.getOrNull(2)?.let { OverviewTile(it, total, 2, Modifier.fillMaxWidth().height((firstRowHeight - 8.dp) / 2)) }
                }
            }
            if (items.size > 3) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.drop(3).take(3).forEachIndexed { localIndex, item ->
                        OverviewTile(item, total, localIndex + 3, Modifier.weight(1f).height(secondRowHeight))
                    }
                }
            }
            if (items.size > 6) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.drop(6).take(2).forEachIndexed { localIndex, item ->
                        OverviewTile(item, total, localIndex + 6, Modifier.weight(1f).height(82.dp))
                    }
                }
            }
            if (!finanza) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(LocalAppExperienceTokens.current.cardRadius - 5.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    border = if (modern || web) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Entradas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(income, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Saídas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(spent, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Disponível", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(balance, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTile(
    item: OverviewMosaicItem,
    total: Double,
    index: Int,
    modifier: Modifier,
    large: Boolean = false
) {
    val tokens = LocalAppExperienceTokens.current
    val modern = LocalAppExperience.current == AppExperience.NEXT
    val web = LocalAppExperience.current == AppExperience.WEB
    val colors = subscriptionTileColors()
    val share = if (total > 0.0) ((item.amount / total) * 100.0).toInt().coerceAtLeast(1) else 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.cardRadius - if (large) 3.dp else 6.dp),
        color = colors[(item.colorIndex + index) % colors.size],
        border = if (modern || web) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.58f))
    ) {
        Column(Modifier.padding(if (large) 14.dp else 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.68f)) {
                    Icon(item.icon, contentDescription = null, modifier = Modifier.padding(6.dp).size(if (large) 18.dp else 14.dp), tint = Color(0xFF111827))
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.76f)) {
                    Text("$share%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(item.title, style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall, color = Color(0xFF111827), fontWeight = FontWeight.Bold, maxLines = if (large) 2 else 1)
            Text(moneyLabel(item.amount), style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall, color = Color(0xFF020617), fontWeight = FontWeight.Black, maxLines = 1)
            Text(item.detail, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4B5563), maxLines = 1)
        }
    }
}

@Composable
private fun DashboardManagerHeader(active: Int, total: Int, onEdit: () -> Unit) {
    val finanza = LocalAppExperience.current.usesFinanzaVisuals
    if (finanza) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Widgets do painel",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Tune, contentDescription = "Personalizar painel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$active de $total widgets",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = "Personalizar painel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardWorkbench(
    onAccounts: () -> Unit,
    onAnalysis: () -> Unit,
    onFeatures: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit
) {
    if (LocalAppExperience.current == AppExperience.NEXT) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onFeatures)
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFeatures, modifier = Modifier.size(46.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = "Abrir recursos",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(9.dp).size(19.dp)
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text("Central de recursos", style = MaterialTheme.typography.titleSmall)
                Text("Planejamento, listas e veiculos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onHistory) {
                Icon(Icons.Rounded.History, contentDescription = "Abrir histórico", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Abrir recursos", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    WidgetSurface("Atalhos do Finanza", "O que você usa sempre", Icons.Rounded.SpaceDashboard) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetAction("Contas", Icons.Rounded.AccountBalanceWallet, onAccounts, Modifier.weight(1f))
            WidgetAction("Análise", Icons.Rounded.Analytics, onAnalysis, Modifier.weight(1f))
            WidgetAction("Recursos", Icons.Rounded.GridView, onFeatures, Modifier.weight(1f))
            WidgetAction("Ajustes", Icons.Rounded.Settings, onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardQuickActions(onAdd: () -> Unit, onTransactions: () -> Unit, onAccounts: () -> Unit) {
    WidgetSurface("Ações rápidas", "Menos toques para o dia a dia", Icons.Rounded.Bolt) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetAction("Nova", Icons.Rounded.Add, onAdd, Modifier.weight(1f), true)
            WidgetAction("Gastos", Icons.Rounded.History, onTransactions, Modifier.weight(1f))
            WidgetAction("Contas", Icons.Rounded.AccountBalanceWallet, onAccounts, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardBills(items: List<BillUi>, onBill: (Long) -> Unit, onAll: () -> Unit) {
    WidgetSurface("Próximos vencimentos", "O que pede atenção", Icons.AutoMirrored.Rounded.ReceiptLong, onAll) {
        if (items.isEmpty()) WidgetEmpty("Nenhum vencimento pendente.")
        else items.take(3).forEach { BillRow(it, { onBill(it.id) }) }
    }
}

@Composable
private fun DashboardRenewals(state: FeatureCenterUiState, onFeatures: () -> Unit) {
    val modules = listOf("subscriptions", "debts", "contracts")
    val items = modules.flatMap { id -> state.modules.firstOrNull { it.id == id }?.items.orEmpty() }
        .sortedBy { it.subtitle }
    WidgetSurface("Renovações próximas", "Assinaturas, dívidas e contratos", Icons.Rounded.Subscriptions, onFeatures) {
        if (items.isEmpty()) {
            WidgetEmpty("Nenhuma renovação ou parcela cadastrada.")
        } else {
            items.take(3).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(item.value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DashboardMiniStats(items: List<TransactionUi>, onAll: () -> Unit) {
    val expenses = items.filterNot { it.income }
    val largest = expenses.maxByOrNull { moneyNumber(it.amount) }
    val activeDays = expenses.map { it.dateKey }.distinct().size
    val average = if (activeDays == 0) 0.0 else expenses.sumOf { moneyNumber(it.amount) } / activeDays
    WidgetSurface("Mini estatísticas", "Leitura rápida dos gastos visíveis", Icons.Rounded.Analytics, onAll) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowMetric("Por dia", moneyLabel(average), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FlowMetric("Maior", largest?.amount ?: "R$ 0,00", MaterialTheme.colorScheme.error, Modifier.weight(1f))
            FlowMetric("Dias", activeDays.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardSavingsRate(items: List<TransactionUi>, onAnalysis: () -> Unit) {
    val income = items.filter { it.income }.sumOf { moneyNumber(it.amount) }
    val expenses = items.filterNot { it.income }.sumOf { moneyNumber(it.amount) }
    val balance = income - expenses
    val rate = if (income <= 0.0) 0.0 else (balance / income * 100.0)
    WidgetSurface("Taxa de economia", "Quanto das receitas ainda está disponível", Icons.AutoMirrored.Rounded.ShowChart, onAnalysis) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowMetric("Taxa", String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.0f%%", rate), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FlowMetric("Sobra", moneyLabel(balance), if (balance >= 0.0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardSubscriptions(state: FeatureCenterUiState, onAll: () -> Unit) {
    val module = state.modules.firstOrNull { it.id == "subscriptions" }
    val items = module?.items.orEmpty()
        .asSequence()
        .filter { it.status.equals("active", ignoreCase = true) }
        .sortedByDescending { moneyNumber(it.value) }
        .take(8)
        .toList()
    val amounts = items.map { moneyNumber(it.value) }
    val total = amounts.sum()
    WidgetSurface("Assinaturas", "Peso mensal e projecao anual", Icons.Rounded.Subscriptions, onAll) {
        if (items.isEmpty()) {
            WidgetEmpty(if (module?.items.orEmpty().isEmpty()) {
                module?.emptyText ?: "Suas assinaturas recorrentes aparecerão aqui."
            } else {
                "Nenhuma assinatura ativa no momento."
            })
        } else {
            val firstRowHeight = if (LocalAppExperienceTokens.current.denseLists) 132.dp else 148.dp
            val secondRowHeight = if (LocalAppExperienceTokens.current.denseLists) 108.dp else 120.dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubscriptionTile(
                    item = items[0],
                    amount = amounts.getOrElse(0) { 0.0 },
                    total = total,
                    index = 0,
                    modifier = Modifier.weight(1.08f).height(firstRowHeight),
                    large = true
                )
                Column(Modifier.weight(0.92f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.getOrNull(1)?.let {
                        SubscriptionTile(it, amounts.getOrElse(1) { 0.0 }, total, 1, Modifier.fillMaxWidth().height((firstRowHeight - 8.dp) / 2))
                    }
                    items.getOrNull(2)?.let {
                        SubscriptionTile(it, amounts.getOrElse(2) { 0.0 }, total, 2, Modifier.fillMaxWidth().height((firstRowHeight - 8.dp) / 2))
                    }
                }
            }
            if (items.size > 3) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.drop(3).take(3).forEachIndexed { localIndex, item ->
                        val index = localIndex + 3
                        SubscriptionTile(
                            item = item,
                            amount = amounts.getOrElse(index) { 0.0 },
                            total = total,
                            index = index,
                            modifier = Modifier.weight(1f).height(secondRowHeight)
                        )
                    }
                }
            }
            if (items.size > 6) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.drop(6).take(2).forEachIndexed { localIndex, item ->
                        val index = localIndex + 6
                        SubscriptionTile(
                            item = item,
                            amount = amounts.getOrElse(index) { 0.0 },
                            total = total,
                            index = index,
                            modifier = Modifier.weight(1f).height(82.dp)
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(LocalAppExperienceTokens.current.cardRadius - 5.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total / mes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(moneyLabel(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Projecao anual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(moneyLabel(total * 12.0), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionTile(
    item: FeatureItemUi,
    amount: Double,
    total: Double,
    index: Int,
    modifier: Modifier,
    large: Boolean = false
) {
    val tokens = LocalAppExperienceTokens.current
    val colors = subscriptionTileColors()
    val share = if (total > 0.0) ((amount / total) * 100.0).toInt().coerceAtLeast(1) else 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.cardRadius - if (large) 3.dp else 6.dp),
        color = colors[index % colors.size],
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.58f))
    ) {
        Column(Modifier.padding(if (large) 14.dp else 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.68f)) {
                    Icon(Icons.Rounded.Subscriptions, contentDescription = null, modifier = Modifier.padding(6.dp).size(if (large) 18.dp else 14.dp), tint = Color(0xFF111827))
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.76f)) {
                    Text("$share%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(item.title, style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall, color = Color(0xFF111827), fontWeight = FontWeight.Bold, maxLines = if (large) 2 else 1)
            Text(item.value.ifBlank { moneyLabel(amount) }, style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall, color = Color(0xFF020617), fontWeight = FontWeight.Black, maxLines = 1)
            Text("~${moneyLabel(amount * 12.0)}/ano", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4B5563), maxLines = 1)
        }
    }
}

@Composable
private fun DashboardRecent(items: List<TransactionUi>, onItem: (Long) -> Unit, onAll: () -> Unit) {
    WidgetSurface("Últimas transações", "Histórico recente", Icons.Rounded.History, onAll) {
        if (items.isEmpty()) WidgetEmpty("Nenhuma transação ainda.")
        else items.take(6).forEach { TransactionRow(it, { onItem(it.id) }) }
    }
}

@Composable
private fun DashboardAccounts(items: List<AccountUi>, onAll: () -> Unit) {
    WidgetSurface("Contas", "Saldos por conta", Icons.Rounded.AccountBalanceWallet, onAll) {
        if (items.isEmpty()) WidgetEmpty("Cadastre uma conta para acompanhar os saldos.")
        else items.take(5).forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                    Text(item.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(item.amount, fontWeight = FontWeight.SemiBold)
            }
            if (index < items.take(5).lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DashboardFeatureList(title: String, state: FeatureCenterUiState, moduleId: String, accent: Color, onAll: () -> Unit) {
    val items = state.modules.firstOrNull { it.id == moduleId }?.items.orEmpty()
    WidgetSurface(title, "Visão rápida", if (moduleId == "goals") Icons.Rounded.Flag else Icons.Rounded.TrackChanges, onAll) {
        if (items.isEmpty()) WidgetEmpty("Nenhum item cadastrado.")
        else items.take(4).forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium)
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    item.progress?.let { value ->
                        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(5.dp).clip(CircleShape)) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.matchParentSize()) {}
                            Surface(color = accent, modifier = Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(5.dp)) {}
                        }
                    }
                }
                if (item.value.isNotBlank()) Text(item.value, color = accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun DashboardCategories(items: List<CategoryUi>, onAll: () -> Unit) {
    WidgetSurface("Ranking de gastos", "Categorias com maior peso", Icons.Rounded.BarChart, onAll) {
        if (items.isEmpty()) WidgetEmpty("Os gastos por categoria aparecerão aqui.")
        else items.take(6).forEach { item ->
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(item.amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp).clip(CircleShape)) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.matchParentSize()) {}
                Surface(color = item.color, modifier = Modifier.fillMaxWidth(item.share.coerceIn(0f, 1f)).height(5.dp)) {}
            }
        }
    }
}

@Composable
private fun WidgetSurface(title: String, subtitle: String, icon: ImageVector, onOpen: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current.usesFinanzaVisuals
    val modern = LocalAppExperience.current == AppExperience.NEXT
    val web = LocalAppExperience.current == AppExperience.WEB
    val modernDark = modern && MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        shape = RoundedCornerShape(tokens.cardRadius),
        color = if (modernDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f) else MaterialTheme.colorScheme.surface,
        border = if (modern) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (web) 0.06f else 1f)),
        shadowElevation = if (finanza) 2.dp else 0.dp
    ) {
        Column(Modifier.padding(if (finanza) 16.dp else if (tokens.denseLists) 14.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (finanza) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(if (finanza) 7.dp else 0.dp).size(19.dp)
                    )
                }
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                onOpen?.let { IconButton(onClick = it) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Abrir") } }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun WidgetAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val tokens = LocalAppExperienceTokens.current
    Surface(
        modifier = modifier.height(if (tokens.denseLists) 62.dp else 66.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(tokens.cardRadius - 5.dp),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        }
    }
}

@Composable
private fun WidgetEmpty(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
private fun DashboardWidgetManager(
    config: DashboardWidgetConfig,
    experience: AppExperience,
    onDismiss: () -> Unit,
    onUpdate: (DashboardWidgetConfig) -> Unit
) {
    val maxHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.64f
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val tokens = LocalAppExperienceTokens.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = tokens.sheetRadius, topEnd = tokens.sheetRadius)) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 28.dp)) {
            Text("Editar widgets", style = MaterialTheme.typography.titleLarge)
            Text("Escolha os blocos e a ordem da página inicial.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { onUpdate(defaultDashboardWidgetConfig(experience, focused = true)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(tokens.cardRadius - 6.dp)) { Text("Essencial") }
                OutlinedButton(onClick = { onUpdate(config.copy(active = dashboardWidgetDefs.map { it.id }.toSet())) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(tokens.cardRadius - 6.dp)) { Text("Todos") }
                IconButton(onClick = { onUpdate(defaultDashboardWidgetConfig(experience)) }) { Icon(Icons.Rounded.Restore, contentDescription = "Restaurar") }
            }
            dashboardWidgetDefs.forEach { definition ->
                val index = config.order.indexOf(definition.id)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(tokens.cardRadius - 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (tokens.denseLists) 0.52f else 0.64f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(definition.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(definition.title, style = MaterialTheme.typography.labelLarge)
                                if (definition.pinned) Icon(Icons.Rounded.PushPin, contentDescription = "Fixo", modifier = Modifier.padding(start = 5.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${definition.group} | ${definition.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        if (!definition.pinned) {
                            IconButton(onClick = { onUpdate(config.copy(order = moveDashboardWidget(config.order, definition.id, -1))) }, enabled = index > 1) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Subir") }
                            IconButton(onClick = { onUpdate(config.copy(order = moveDashboardWidget(config.order, definition.id, 1))) }, enabled = index in 1 until config.order.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Descer") }
                            Switch(
                                checked = definition.id in config.active,
                                onCheckedChange = { enabled ->
                                    val next = if (enabled) config.active + definition.id else config.active - definition.id
                                    if (enabled || next.size >= 3) onUpdate(config.copy(active = next))
                                }
                            )
                        }
                    }
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(tokens.cardRadius - 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Concluir")
            }
        }
    }
}

private fun moveDashboardWidget(order: List<String>, id: String, direction: Int): List<String> {
    val from = order.indexOf(id)
    val to = from + direction
    if (from <= 0 || to <= 0 || to !in order.indices) return order
    return order.toMutableList().apply { add(to, removeAt(from)) }
}

private fun defaultDashboardWidgetConfig(experience: AppExperience = AppExperience.FINANZA, focused: Boolean = false): DashboardWidgetConfig {
    val focusedIds = setOf("cards", "commitments", "quickactions", "recent")
    val isModern = experience == AppExperience.NEXT
    val active = dashboardWidgetDefs.filter {
        it.pinned || if (isModern) it.id in focusedIds else if (focused) it.id in focusedIds else it.default
    }.map { it.id }.toSet()
    return DashboardWidgetConfig(active, dashboardWidgetDefs.map { it.id })
}

private fun normalizeDashboardWidgetConfig(config: DashboardWidgetConfig): DashboardWidgetConfig {
    val ids = dashboardWidgetDefs.map { it.id }
    val active = (config.active.intersect(ids.toSet()) + "workbench").toMutableSet()
    dashboardWidgetDefs.filter { it.default }.forEach { if (active.size < 3) active += it.id }
    val order = listOf("workbench") + config.order.filter { it in ids && it != "workbench" }.distinct() + ids.filter { it !in config.order && it != "workbench" }
    return DashboardWidgetConfig(active, order)
}

private fun loadDashboardWidgetConfig(context: Context, experience: AppExperience): DashboardWidgetConfig {
    val prefs = FinanzaPreferences.get(context)
    val fallback = defaultDashboardWidgetConfig(experience)
    if (experience == AppExperience.NEXT) {
        val activeRaw = prefs.getString("dashboard_widget_active_next", null) ?: return fallback
        return runCatching {
            val activeArray = JSONArray(activeRaw)
            val active = (0 until activeArray.length()).map(activeArray::getString).toSet()
            val orderArray = JSONArray(prefs.getString("dashboard_widget_order_next", null) ?: return fallback)
            normalizeDashboardWidgetConfig(DashboardWidgetConfig(active, (0 until orderArray.length()).map(orderArray::getString)))
        }.getOrDefault(fallback)
    }
    val suffix = if (experience == AppExperience.WEB) "_web" else ""
    return runCatching {
        val activeRaw = prefs.getString("dashboard_widget_active$suffix", null)
        val webPrefs = runCatching { JSONObject(prefs.getString("widget_prefs", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val active = if (activeRaw != null) {
            val array = JSONArray(activeRaw)
            (0 until array.length()).map(array::getString).toSet()
        } else dashboardWidgetDefs.filter { webPrefs.optBoolean(it.id, it.default) || it.pinned }.map { it.id }.toSet()
        val orderArray = JSONArray(prefs.getString("dashboard_widget_order$suffix", prefs.getString("widget_order", null)) ?: return fallback)
        // Versions before the overview used this dashboard position exclusively for subscriptions.
        val parsedOrder = (0 until orderArray.length()).map(orderArray::getString)
            .map { if (it == "subscriptions") "overview" else it }
            .distinct()
        val migratedActive = (active - "subscriptions" + "overview") + dashboardWidgetDefs
            .filter { it.default && it.id !in parsedOrder }
            .map { it.id }
        normalizeDashboardWidgetConfig(
            DashboardWidgetConfig(
                migratedActive,
                parsedOrder
            )
        )
    }.getOrDefault(fallback)
}

private fun saveDashboardWidgetConfig(context: Context, experience: AppExperience, config: DashboardWidgetConfig) {
    val webPrefs = JSONObject().apply { dashboardWidgetDefs.forEach { put(it.id, it.id in config.active) } }
    val suffix = when (experience) {
        AppExperience.NEXT -> "_next"
        AppExperience.WEB -> "_web"
        AppExperience.FINANZA -> ""
    }
    FinanzaPreferences.get(context).edit().apply {
        putString("dashboard_widget_active$suffix", JSONArray(config.active.toList()).toString())
        putString("dashboard_widget_order$suffix", JSONArray(config.order).toString())
        if (experience == AppExperience.FINANZA) {
            putString("widget_prefs", webPrefs.toString())
            putString("widget_order", JSONArray(config.order).toString())
        }
    }.apply()
}

@Composable
private fun subscriptionTileColors(): List<Color> {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) listOf(
        Color(0xFFBFC7FF),
        Color(0xFF98F5DF),
        Color(0xFFFFC1CD),
        Color(0xFFFFD6A5),
        Color(0xFFC6F6D5),
        Color(0xFFE7C6FF),
        Color(0xFFFFF1A8),
        Color(0xFFC7E7FF)
    ) else listOf(
        Color(0xFFDDE3FF),
        Color(0xFFC4FFF0),
        Color(0xFFFFD4DE),
        Color(0xFFFFE0B5),
        Color(0xFFD2FBE2),
        Color(0xFFEED8FF),
        Color(0xFFFFF4BE),
        Color(0xFFD8EEFF)
    )
}

private fun moneyNumber(raw: String): Double {
    val cleaned = raw
        .replace("R$", "")
        .replace(".", "")
        .replace(",", ".")
        .filter { it.isDigit() || it == '.' || it == '-' }
    return cleaned.toDoubleOrNull() ?: 0.0
}

private fun moneyLabel(value: Double): String {
    val cents = kotlin.math.round(value * 100.0).toLong()
    val sign = if (cents < 0) "-" else ""
    val absolute = kotlin.math.abs(cents)
    val reais = absolute / 100
    val centavos = (absolute % 100).toString().padStart(2, '0')
    return "$sign R$ $reais,$centavos".replace("- R$", "-R$")
}
