package com.finanza.next.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience

@Composable
fun HomeScreen(
    userName: String,
    greeting: String,
    period: String,
    balance: String,
    income: String,
    spent: String,
    transactions: List<TransactionUi>,
    accounts: List<AccountUi>,
    bills: List<BillUi>,
    categories: List<CategoryUi>,
    features: FeatureCenterUiState,
    onAdd: () -> Unit,
    onAll: () -> Unit,
    onTransaction: (Long) -> Unit,
    onBill: (Long) -> Unit,
    onFeatures: () -> Unit,
    onAccounts: () -> Unit,
    onAnalysis: () -> Unit,
    onSettings: () -> Unit
) {
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    var editingDashboard by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 108.dp)
    ) {
        item {
            if (finanza) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        Text(
                            "Visão geral das suas finanças",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    IconButton(onClick = { editingDashboard = true }) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = "Personalizar painel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            period,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                    Button(
                        onClick = onAdd,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Nova", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            } else {
                Column {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f))
                    Text(userName, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            DashboardWidgets(
                period = period,
                balance = balance,
                income = income,
                spent = spent,
                transactions = transactions,
                accounts = accounts,
                bills = bills,
                categories = categories,
                features = features,
                onAdd = onAdd,
                onAllTransactions = onAll,
                onTransaction = onTransaction,
                onBill = onBill,
                onAccounts = onAccounts,
                onAnalysis = onAnalysis,
                onFeatures = onFeatures,
                onSettings = onSettings,
                showManagerHeader = !finanza,
                managerVisible = editingDashboard,
                onManagerVisibleChange = { editingDashboard = it }
            )
        }
    }
}
