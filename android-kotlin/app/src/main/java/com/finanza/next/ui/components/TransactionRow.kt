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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.DangerRed
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import com.finanza.next.ui.theme.SuccessGreen

data class TransactionUi(
    val id: Long,
    val title: String,
    val category: String,
    val amount: String,
    val date: String,
    val income: Boolean,
    val color: Color
)

@Composable
fun TransactionRow(item: TransactionUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    val amountColor = when {
        item.income && finanza -> MaterialTheme.colorScheme.primary
        item.income -> SuccessGreen
        finanza -> MaterialTheme.colorScheme.error
        else -> DangerRed
    }
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = if (tokens.denseLists) 11.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(item.color.copy(alpha = if (tokens.denseLists) 0.16f else 0.12f)), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(item.category), null, tint = item.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (finanza) {
                Box(
                    Modifier.padding(top = 4.dp).clip(CircleShape)
                        .background(item.color.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(item.category, style = MaterialTheme.typography.labelSmall, color = item.color, maxLines = 1)
                }
            } else {
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.amount, fontWeight = FontWeight.SemiBold, color = amountColor)
            Text(item.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
        }
    }
}
