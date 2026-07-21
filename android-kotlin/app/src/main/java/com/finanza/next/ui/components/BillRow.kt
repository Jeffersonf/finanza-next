package com.finanza.next.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

data class BillUi(val id: Long, val name: String, val category: String, val due: String, val amount: String, val status: BillStatus)
data class AccountUi(val id: String, val name: String, val type: String, val amount: String, val iconKey: String)

@Composable
fun BillRow(item: BillUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.cardRadius)).background(if (finanza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(if (tokens.denseLists) 15.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (finanza) 0.16f else 1f)), contentAlignment = Alignment.Center) {
            Text(item.name.take(1).uppercase(), color = if (finanza) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text("${item.category}  |  ${item.due}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.amount, style = MaterialTheme.typography.bodyMedium)
            StatusBadge(item.status)
        }
    }
}

@Composable
fun AccountRow(item: AccountUi, onClick: () -> Unit) {
    val tokens = LocalAppExperienceTokens.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = if (tokens.denseLists) 12.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (tokens.denseLists) 0.12f else 0.10f)), contentAlignment = Alignment.Center) {
            Icon(accountIcon(item.iconKey), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text(accountSubtitle(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
        }
        Text(item.amount, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun accountSubtitle(item: AccountUi): String {
    if (!item.name.equals(item.type, ignoreCase = true)) return item.type

    return when (item.type.lowercase()) {
        "cartao", "cartão" -> "Cartão de crédito"
        "reserva" -> "Conta de reserva"
        "investimentos" -> "Carteira de investimentos"
        else -> item.type
    }
}
