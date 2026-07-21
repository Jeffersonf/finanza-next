package com.finanza.next.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience

@Composable
fun CircularProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val normalized = progress.coerceIn(0f, 1f)
    val progressColor = when (LocalAppExperience.current) {
        AppExperience.NEXT -> Color(0xFFD2F668)
        AppExperience.FINANZA -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    Box(modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(64.dp)) {
            drawArc(trackColor, -90f, 360f, false, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            drawArc(progressColor, -90f, 360f * normalized, false, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("${(normalized * 100).toInt()}%", color = labelColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}
