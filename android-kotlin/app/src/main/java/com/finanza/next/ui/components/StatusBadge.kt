package com.finanza.next.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.DangerRed
import com.finanza.next.ui.theme.DangerRedBg
import com.finanza.next.ui.theme.SuccessGreen
import com.finanza.next.ui.theme.SuccessGreenBg
import com.finanza.next.ui.theme.WarningOrange
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

enum class BillStatus { ATRASADO, PAGO, PENDENTE }

@Composable
fun StatusBadge(status: BillStatus, modifier: Modifier = Modifier) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    val colors = when (status) {
        BillStatus.ATRASADO -> (if (finanza) MaterialTheme.colorScheme.error.copy(alpha = 0.14f) else DangerRedBg) to (if (finanza) MaterialTheme.colorScheme.error else DangerRed)
        BillStatus.PAGO -> (if (finanza) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else SuccessGreenBg) to (if (finanza) MaterialTheme.colorScheme.primary else SuccessGreen)
        BillStatus.PENDENTE -> WarningOrange.copy(alpha = 0.14f) to WarningOrange
    }
    val label = when (status) {
        BillStatus.ATRASADO -> "Atrasado"
        BillStatus.PAGO -> "Pago"
        BillStatus.PENDENTE -> "Pendente"
    }
    Text(
        text = label,
        color = colors.second,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.clip(RoundedCornerShape(if (tokens.denseLists) 7.dp else 9.dp)).background(colors.first).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
