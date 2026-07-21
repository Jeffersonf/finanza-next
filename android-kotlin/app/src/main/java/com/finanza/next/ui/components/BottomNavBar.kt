package com.finanza.next.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience

enum class AppTab(val label: String) { HOME("Início"), CONTAS("Contas"), ANALISE("Análise"), CONFIG("Ajustes") }

@Composable
fun BottomNavBar(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(AppTab.HOME, Icons.Rounded.Home, "Início"),
        Triple(AppTab.CONTAS, Icons.Rounded.AccountBalanceWallet, "Contas"),
        Triple(AppTab.ANALISE, Icons.Rounded.Insights, "Análise"),
        Triple(AppTab.CONFIG, Icons.Rounded.Settings, "Ajustes")
    )
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tokens = LocalAppExperienceTokens.current

    Surface(
        modifier = modifier.fillMaxWidth().height(tokens.navHeight),
        shape = RoundedCornerShape(tokens.navRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) tokens.glassDarkAlpha else tokens.glassLightAlpha),
        border = BorderStroke(
            1.dp,
            if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.72f)
        ),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.fillMaxSize().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            items.forEach { (tab, icon, label) ->
                NavItem(
                    selected = selected == tab,
                    icon = icon,
                    label = label,
                    dark = dark,
                    showLabel = tokens.showNavLabels,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    dark: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppExperienceTokens.current
    val finanza = LocalAppExperience.current == AppExperience.FINANZA
    val contentColor = when {
        selected && finanza -> MaterialTheme.colorScheme.onPrimary
        selected -> Color.White
        dark -> Color.White.copy(alpha = 0.58f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
    }
    val selectedColor = when {
        finanza -> MaterialTheme.colorScheme.primary
        dark -> tokens.navSelectedDark
        else -> tokens.navSelectedLight
    }

    Box(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(tokens.navSelectedRadius))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(18.dp))
            if (showLabel) {
                Text(
                    label,
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    lineHeight = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}
