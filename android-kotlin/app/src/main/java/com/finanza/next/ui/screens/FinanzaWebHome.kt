package com.finanza.next.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.TransactionUi
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun FinanzaWebHome(
    period: String,
    balance: String,
    income: String,
    monthlyIncome: String,
    spent: String,
    transactions: List<TransactionUi>,
    accounts: List<AccountUi>,
    bills: List<BillUi>,
    categories: List<CategoryUi>,
    trends: List<MonthTrendUi>,
    features: FeatureCenterUiState,
    onAdd: () -> Unit,
    onAll: () -> Unit,
    onTransaction: (Long) -> Unit,
    onBill: (Long) -> Unit,
    onFeatures: () -> Unit,
    onImport: () -> Unit,
    onAccounts: () -> Unit,
    onAnalysis: () -> Unit,
    onSettings: () -> Unit
) {
    var editingDashboard by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Início", style = MaterialTheme.typography.headlineMedium, maxLines = 1)
                Text(
                    "Centro do dia para lançar, revisar e decidir seus gastos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.06f))
                ) {
                    Text(
                        period.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)
                    )
                }
                IconButton(onClick = { editingDashboard = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Personalizar painel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onImport) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "Abrir central de importação", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("+ Nova")
                }
                }
            }
        }
        item { FinanzaWebSummaryGrid(balance, income, monthlyIncome, spent, transactions, bills, onSettings, onAll, onAccounts) }
        item {
            DashboardWidgets(
                period = period,
                balance = balance,
                income = income,
                spent = spent,
                transactions = transactions,
                accounts = accounts,
                bills = bills,
                categories = categories,
                trends = trends,
                features = features,
                onAdd = onAdd,
                onAllTransactions = onAll,
                onTransaction = onTransaction,
                onBill = onBill,
                onAccounts = onAccounts,
                onAnalysis = onAnalysis,
                onFeatures = onFeatures,
                onSettings = onSettings,
                hiddenWidgetIds = setOf("cards", "workbench"),
                showManagerHeader = false,
                managerVisible = editingDashboard,
                onManagerVisibleChange = { editingDashboard = it }
            )
        }
    }
}

@Composable
private fun FinanzaWebSummaryGrid(
    balance: String,
    income: String,
    monthlyIncome: String,
    spent: String,
    transactions: List<TransactionUi>,
    bills: List<BillUi>,
    onSettings: () -> Unit,
    onTransactions: () -> Unit,
    onBills: () -> Unit
) {
    val currentMonth = YearMonth.now()
    val unsettled = transactions.filter { transaction ->
        !transaction.paid && runCatching { YearMonth.from(LocalDate.parse(transaction.dateKey)) == currentMonth }.getOrDefault(false)
    }
    // The web dashboard's flow widgets use open movements only; settled entries remain
    // visible in history but do not inflate the current-period projection.
    val liveIncome = unsettled.filter { it.income }.sumOf { finanzaWebMoneyNumber(it.amount) }
    val liveSpent = unsettled.filterNot { it.income }.sumOf { finanzaWebMoneyNumber(it.amount) }
    val reportedIncome = finanzaWebMoneyNumber(income)
    val configuredIncome = finanzaWebMoneyNumber(monthlyIncome)
    val baseIncome = configuredIncome.takeIf { it > 0.0 }
        ?: liveIncome.takeIf { unsettled.isNotEmpty() }
        ?: reportedIncome
    val monthlySpent = if (unsettled.isNotEmpty()) liveSpent else finanzaWebMoneyNumber(spent)
    val remaining = baseIncome - monthlySpent
    val daysLeft = (YearMonth.now().lengthOfMonth() - LocalDate.now().dayOfMonth + 1).coerceAtLeast(1)
    val safePerDay = (remaining / daysLeft).coerceAtLeast(0.0)
    val dueTotal = bills.sumOf { finanzaWebMoneyNumber(it.amount) }
    val incomeCard = FinanzaWebSummary(
        "Entradas", finanzaWebMoney(baseIncome),
        if (configuredIncome > 0.0) "renda mensal configurada" else "recebido neste mês",
        Icons.Rounded.AccountBalanceWallet, MaterialTheme.colorScheme.secondary, onSettings
    )
    val spentCard = FinanzaWebSummary(
        "Saídas", finanzaWebMoney(monthlySpent),
        if (baseIncome > 0.0) "${(monthlySpent / baseIncome * 100).toInt()}% da renda" else "sem renda registrada",
        Icons.Rounded.TrendingDown, MaterialTheme.colorScheme.error, onTransactions
    )
    val remainingCard = FinanzaWebSummary(
        "Sobra projetada", finanzaWebMoney(remaining),
        "${finanzaWebMoney(baseIncome)} recebido no mês",
        Icons.Rounded.TrendingUp,
        if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        onTransactions
    )
    val dueCard = FinanzaWebSummary(
        "A pagar", finanzaWebMoney(dueTotal), "ver contas futuras",
        Icons.Rounded.CalendarMonth, MaterialTheme.colorScheme.tertiary, onBills
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FinanzaWebBalanceHero(balance)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanzaWebSummaryCard(incomeCard, Modifier.weight(1f), compact = true)
            FinanzaWebSummaryCard(spentCard, Modifier.weight(1f), compact = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanzaWebSummaryCard(remainingCard, Modifier.weight(1f), compact = true)
            FinanzaWebSummaryCard(dueCard, Modifier.weight(1f), compact = true)
        }
    }
}

@Composable
private fun FinanzaWebBalanceHero(balance: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Text("Saldo disponível", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(balance, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
            Text("Em todas as contas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

private data class FinanzaWebSummary(
    val label: String,
    val value: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
private fun FinanzaWebSummaryCard(summary: FinanzaWebSummary, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = summary.onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.06f))
    ) {
        Column(Modifier.padding(if (compact) 13.dp else 15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(if (compact) 34.dp else 38.dp).clip(RoundedCornerShape(11.dp)).background(summary.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(summary.icon, contentDescription = null, tint = summary.accent, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
            }
            Column {
                Text(summary.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(summary.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = summary.accent, maxLines = 1)
                Text(summary.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

private fun finanzaWebMoneyNumber(raw: String): Double = raw
    .replace("R$", "", ignoreCase = true)
    .replace(".", "")
    .replace(',', '.')
    .trim()
    .toDoubleOrNull() ?: 0.0

private fun finanzaWebMoney(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
