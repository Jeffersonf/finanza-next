package com.finanza.next.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.HeroCard
import com.finanza.next.ui.theme.HeroCardSecondaryText
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import com.finanza.next.ui.theme.TextPrimaryDark

@Composable
fun HeroBalanceCard(label: String, period: String, value: String, entradas: String, saidas: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    val radius = RoundedCornerShape(tokens.cardRadius + 8.dp)
    val cardColor = when {
        finanza -> MaterialTheme.colorScheme.surfaceVariant
        tokens.denseLists -> HeroCard
        else -> MaterialTheme.colorScheme.inverseSurface
    }
    val mutedColor = when {
        finanza -> MaterialTheme.colorScheme.onSurfaceVariant
        tokens.denseLists -> HeroCardSecondaryText
        else -> MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.62f)
    }
    val contentColor = when {
        finanza -> MaterialTheme.colorScheme.onSurface
        tokens.denseLists -> TextPrimaryDark
        else -> MaterialTheme.colorScheme.inverseOnSurface
    }
    Column(
        modifier.fillMaxWidth().clip(radius).background(cardColor)
            .border(1.dp, if (finanza) MaterialTheme.colorScheme.outlineVariant else Color.White.copy(alpha = if (tokens.denseLists) 0.10f else 0.16f), radius).padding(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = mutedColor, style = MaterialTheme.typography.labelSmall)
            Text(period.uppercase(), color = mutedColor, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(14.dp))
        Text(value, color = contentColor, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("Entradas", entradas, mutedColor, contentColor)
            Metric("Saídas", saidas, mutedColor, contentColor)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, labelColor: Color, valueColor: Color) {
    Column {
        Text(label, color = labelColor, style = MaterialTheme.typography.labelSmall)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium)
    }
}
