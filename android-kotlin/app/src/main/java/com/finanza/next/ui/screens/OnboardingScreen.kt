package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class OnboardingPage(val title: String, val copy: String, val icon: ImageVector)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    canPinWidget: Boolean = false,
    onRequestWidgetPin: () -> Unit = {}
) {
    val pages = listOf(
        OnboardingPage(
            "Seu dinheiro em um s\u00F3 lugar",
            "Acompanhe saldo, contas, cart\u00F5es e vencimentos desde a tela inicial.",
            Icons.Rounded.SpaceDashboard
        ),
        OnboardingPage(
            "M\u00F3dulos quando precisar",
            "Or\u00E7amentos, metas, ve\u00EDculos, listas e compartilhado ficam reunidos na Central.",
            Icons.Rounded.Widgets
        ),
        OnboardingPage(
            "Registre sem perder tempo",
            "Use o bot\u00E3o de adicionar, o atalho ou a notifica\u00E7\u00E3o para lan\u00E7ar um gasto em segundos.",
            Icons.Rounded.Bolt
        ),
        OnboardingPage(
            "Deixe a captura na tela inicial",
            "Adicione o widget de gasto r\u00E1pido para informar valor e descri\u00E7\u00E3o sem abrir o Finext.",
            Icons.Rounded.Widgets
        )
    )
    var page by rememberSaveable { mutableIntStateOf(0) }
    val current = pages[page]
    val last = page == pages.lastIndex

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Pular",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                modifier = Modifier.clickable(onClick = onComplete).padding(10.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(92.dp).clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(current.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(30.dp))
        Text(current.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text(
            current.copy,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (last) WidgetPreview()
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            pages.indices.forEach { index ->
                Box(
                    Modifier.size(width = if (page == index) 22.dp else 7.dp, height = 7.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (page == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (last) {
                    if (canPinWidget) onRequestWidgetPin()
                    onComplete()
                } else {
                    page += 1
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text(
                when {
                    !last -> "Continuar"
                    canPinWidget -> "Adicionar widget"
                    else -> "Come\u00E7ar"
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            if (last && canPinWidget) "Agora n\u00E3o" else "Pular introdu\u00E7\u00E3o",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
            modifier = Modifier.clickable(onClick = onComplete).padding(top = 14.dp)
        )
    }
}

@Composable
private fun WidgetPreview() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text("Gasto r\u00E1pido", style = MaterialTheme.typography.labelLarge)
                Text("Toque para registrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("R$ 0,00", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 15.dp))
    }
}
