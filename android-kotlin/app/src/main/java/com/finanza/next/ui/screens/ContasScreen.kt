package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.AccountRow
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.BillRow
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.CircularProgressRing
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

@Composable
fun ContasScreen(
    period: String,
    total: String,
    progress: Float,
    paid: String,
    remaining: String,
    accounts: List<AccountUi>,
    bills: List<BillUi>,
    onNewAccount: () -> Unit,
    onTransfer: () -> Unit,
    onAccount: (String) -> Unit,
    onBill: (Long) -> Unit
) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    val heroShape = RoundedCornerShape(tokens.cardRadius + 8.dp)
    val heroColor = if (finanza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.inverseSurface
    val heroMuted = if (finanza) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.62f)
    val heroInk = if (finanza) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 108.dp)
    ) {
        item {
            if (finanza) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Contas", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Corrente, poupança e cartão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Button(onClick = onNewAccount) { Text("+ Nova conta") }
                }
            } else {
                Text("Planejamento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f))
                Text("Contas", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(heroShape).background(heroColor)
                    .border(1.dp, if (finanza) MaterialTheme.colorScheme.outlineVariant else androidx.compose.ui.graphics.Color.Transparent, heroShape)
                    .padding(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (finanza) "VISÃO CONSOLIDADA" else period.uppercase(), color = heroMuted, style = MaterialTheme.typography.labelSmall)
                    Text(total, color = heroInk, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(10.dp))
                    Text(if (finanza) "Fatura paga: $paid" else "$paid pago", color = heroMuted, style = MaterialTheme.typography.bodySmall)
                    Text(if (finanza) "Em aberto: $remaining" else "$remaining restante", color = heroMuted, style = MaterialTheme.typography.bodySmall)
                }
                CircularProgressRing(progress)
            }
            Spacer(Modifier.height(25.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (finanza) "Contas e carteiras" else "Suas contas", style = MaterialTheme.typography.titleMedium)
                Text("${accounts.size} ativas", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f))
            }
            Spacer(Modifier.height(6.dp))
        }
        items(accounts, key = { it.id }) { item -> AccountRow(item) { onAccount(item.id) } }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!finanza) Button(onClick = onNewAccount, modifier = Modifier.weight(1f)) { Text("Nova conta") }
                OutlinedButton(onClick = onTransfer, modifier = if (finanza) Modifier.fillMaxWidth() else Modifier.weight(1f)) { Text("Transferir") }
            }
            Text(if (finanza) "Próximos vencimentos" else "Agenda financeira", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
        }
        if (bills.isEmpty()) item { Text("Nenhum vencimento cadastrado.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f), modifier = Modifier.padding(vertical = 18.dp)) }
        items(bills, key = { it.id }) { item -> BillRow(item, { onBill(item.id) }, Modifier.padding(bottom = 10.dp)) }
    }
}
