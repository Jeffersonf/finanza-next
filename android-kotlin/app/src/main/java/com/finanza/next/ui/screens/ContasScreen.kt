package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.AccountRow
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.BillRow
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.BillStatus
import com.finanza.next.ui.components.CircularProgressRing
import com.finanza.next.ui.components.accountIcon
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
    val experience = LocalAppExperience.current
    val finanza = experience.usesFinanzaVisuals
    val web = experience == AppExperience.WEB
    val heroShape = RoundedCornerShape(tokens.cardRadius + 8.dp)
    val heroColor = if (finanza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.inverseSurface
    val heroMuted = if (finanza) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.62f)
    val heroInk = if (finanza) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface
    LazyColumn(
        Modifier.fillMaxSize().testTag("accountsScreen"),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 108.dp)
    ) {
        item {
            if (finanza) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Contas", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        Text(
                            if (web) "Corrente, poupança, cartão" else "Corrente, poupança e cartão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Button(onClick = onNewAccount) { Text(if (web) "+ Nova Conta" else "+ Nova conta") }
                }
            } else {
                Text("Planejamento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f))
                Text("Contas", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(if (finanza) 16.dp else 18.dp))
            if (!finanza) {
                Row(
                    Modifier.fillMaxWidth().clip(heroShape).background(heroColor)
                        .border(1.dp, androidx.compose.ui.graphics.Color.Transparent, heroShape)
                        .padding(22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(period.uppercase(), color = heroMuted, style = MaterialTheme.typography.labelSmall)
                        Text(total, color = heroInk, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(10.dp))
                        Text("$paid pago", color = heroMuted, style = MaterialTheme.typography.bodySmall)
                        Text("$remaining restante", color = heroMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    CircularProgressRing(progress)
                }
                Spacer(Modifier.height(25.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Suas contas", style = MaterialTheme.typography.titleMedium)
                    Text("${accounts.size} ativas", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f))
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (finanza) {
            items(accounts.chunked(2), key = { row -> row.joinToString(separator = "-") { it.id } }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { item ->
                        if (web) {
                            FinanzaWebAccountTile(item, Modifier.weight(1f)) { onAccount(item.id) }
                        } else {
                            FinanzaAccountTile(item, Modifier.weight(1f)) { onAccount(item.id) }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (web && accounts.any { it.iconKey.equals("credit", ignoreCase = true) || it.type.equals("Cartão", ignoreCase = true) }) {
                item {
                    FinanzaWebCardCenter(
                        cards = accounts.filter { it.iconKey.equals("credit", ignoreCase = true) || it.type.equals("Cartão", ignoreCase = true) },
                        onNewCard = onNewAccount,
                        onCard = onAccount
                    )
                }
            }
        } else {
            items(accounts, key = { it.id }) { item -> AccountRow(item) { onAccount(item.id) } }
        }
        if (web) {
            item {
                FinanzaWebDueSummary(bills)
            }
        }
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

@Composable
private fun FinanzaWebDueSummary(bills: List<BillUi>) {
    val total = bills.sumOf { webBillAmount(it.amount) }
    val overdue = bills.count { it.status == BillStatus.ATRASADO }
    val next = bills.firstOrNull()?.let { "${it.name} • ${it.due}" } ?: "Nenhum vencimento"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WebDueStat("Compromissos", webBillMoney(total), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        WebDueStat("Vencidos", overdue.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
        WebDueStat("Próximo", next, MaterialTheme.colorScheme.primary, Modifier.weight(1.2f))
    }
}

@Composable
private fun WebDueStat(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}

private fun webBillAmount(raw: String): Double = raw
    .replace("R$", "", ignoreCase = true)
    .replace(".", "")
    .replace(',', '.')
    .trim()
    .toDoubleOrNull()
    ?: 0.0

private fun webBillMoney(value: Double): String = java.text.NumberFormat
    .getCurrencyInstance(java.util.Locale("pt", "BR"))
    .format(value)

@Composable
private fun FinanzaAccountTile(item: AccountUi, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tokens = LocalAppExperienceTokens.current
    val shape = RoundedCornerShape(tokens.cardRadius)
    Column(
        modifier = modifier
            .height(148.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(accountIcon(item.iconKey), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            }
            Text(
                accountTypeTag(item.type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)).padding(horizontal = 7.dp, vertical = 4.dp)
            )
        }
        Column {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(item.amount, style = MaterialTheme.typography.titleMedium, color = if (item.amount.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun FinanzaWebAccountTile(item: AccountUi, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val amountColor = if (item.amount.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .height(152.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(accountIcon(item.iconKey), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Text(
                    accountTypeTag(item.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
            Column {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(item.amount, style = MaterialTheme.typography.titleMedium, color = amountColor, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("Saldo disponível", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun FinanzaWebCardCenter(
    cards: List<AccountUi>,
    onNewCard: () -> Unit,
    onCard: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Central de cartões", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Fechamento, vencimento, validade e final do cartão",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            OutlinedButton(onClick = onNewCard) { Text("Novo cartão") }
        }
        Spacer(Modifier.height(12.dp))
        cards.forEachIndexed { index, card ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onCard(card.id) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(accountIcon(card.iconKey), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(card.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            listOfNotNull(
                                card.cardLast4.takeIf { it.isNotBlank() }?.let { "final $it" },
                                card.cardExpiry.takeIf { it.isNotBlank() }?.let { "validade $it" }
                            ).joinToString(" • ").ifBlank { "Dados do cartão" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Fecha ${card.cardClosingDay.takeIf { it in 1..31 } ?: "—"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Vence ${card.cardDueDay.takeIf { it in 1..31 } ?: "—"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun accountTypeTag(type: String): String = when (type.lowercase()) {
    "cartao", "cartão" -> "Crédito"
    "conta corrente" -> "Corrente"
    "conta de reserva", "reserva" -> "Poupança"
    "investimentos", "investimento" -> "Investimento"
    "carteira" -> "Carteira"
    else -> type
}
