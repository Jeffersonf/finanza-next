@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

@Composable
internal fun AppearanceSheet(
    initialMode: String,
    initialExperience: AppExperience,
    onDismiss: () -> Unit,
    onApply: (mode: String, experience: AppExperience) -> Unit
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var experience by remember(initialExperience) { mutableStateOf(initialExperience) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = LocalAppExperienceTokens.current
    val maxHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.76f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).imePadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Aparência", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Escolha o modo e a linguagem visual do Finext.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar")
                }
            }
            }

            item { AppearanceSection("Modo de cor") }
            item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppearanceModeChoice("Claro", mode == "light", experience, Modifier.weight(1f)) { mode = "light" }
                AppearanceModeChoice("Escuro", mode == "dark", experience, Modifier.weight(1f)) { mode = "dark" }
            }
            }

            item { AppearanceSection("Tema") }
            AppExperience.entries.forEach { option ->
                item(key = option.name) {
                AppearanceExperienceChoice(
                    option = option,
                    selected = experience == option,
                    dark = mode == "dark",
                    onClick = { experience = option }
                )
                }
            }

            item {
            Button(
                onClick = { onApply(mode, experience) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(tokens.cardRadius.coerceAtMost(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Aplicar aparência")
            }
            }
        }
    }
}

@Composable
private fun AppearanceSection(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 3.dp)
    )
}

@Composable
private fun AppearanceModeChoice(
    label: String,
    selected: Boolean,
    previewExperience: AppExperience,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val dark = label == "Escuro"
    val preview = appearancePreview(previewExperience, dark)
    val customDarkSelection = selected && dark && previewExperience != AppExperience.FINANZA
    val selectedColor = if (customDarkSelection) {
        preview.surface
    } else {
        preview.accent
    }
    val selectedContentColor = if (customDarkSelection) {
        preview.onSurface
    } else {
        preview.onAccent
    }
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) selectedColor else preview.surface,
        contentColor = if (selected) selectedContentColor else preview.onSurface
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AppearanceExperienceChoice(
    option: AppExperience,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalAppExperienceTokens.current
    val shape = RoundedCornerShape(tokens.cardRadius.coerceAtMost(16.dp))
    val preview = appearancePreview(option, dark)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) preview.accent.copy(alpha = if (dark) 0.20f else 0.13f) else preview.surface,
        contentColor = preview.onSurface,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, preview.accent.copy(alpha = 0.62f)) else null
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(99.dp)).background(preview.canvas).padding(4.dp)) {
                Spacer(Modifier.size(9.dp).background(preview.surface, RoundedCornerShape(99.dp)))
                Spacer(Modifier.size(3.dp))
                Spacer(Modifier.size(9.dp).background(preview.accent, RoundedCornerShape(99.dp)))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(option.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(option.description, style = MaterialTheme.typography.bodySmall, color = preview.muted)
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selecionado", tint = preview.accent)
        }
    }
}

private data class AppearancePreview(
    val canvas: Color,
    val surface: Color,
    val accent: Color,
    val onSurface: Color,
    val onAccent: Color,
    val muted: Color
)

private fun appearancePreview(experience: AppExperience, dark: Boolean): AppearancePreview = when (experience) {
    AppExperience.NEXT -> if (dark) AppearancePreview(
        canvas = Color(0xFF000000), surface = Color(0xFF1C1C1E), accent = Color(0xFFF5F5F7),
        onSurface = Color(0xFFF5F5F7), onAccent = Color.Black, muted = Color(0xFFA1A1AA)
    ) else AppearancePreview(
        canvas = Color(0xFFF2F2F7), surface = Color.White, accent = Color(0xFF1C1C1E),
        onSurface = Color(0xFF1C1C1E), onAccent = Color.White, muted = Color(0xFF6B6B75)
    )
    AppExperience.FINANZA -> if (dark) AppearancePreview(
        canvas = Color(0xFF10150A), surface = Color(0xFF1B2116), accent = Color(0xFFC8F55A),
        onSurface = Color(0xFFF0F4E8), onAccent = Color(0xFF10150A), muted = Color(0xFFB9C1AC)
    ) else AppearancePreview(
        canvas = Color(0xFFF6F7F2), surface = Color(0xFFFFFFFF), accent = Color(0xFF355E20),
        onSurface = Color(0xFF172016), onAccent = Color.White, muted = Color(0xFF62705F)
    )
    AppExperience.WEB -> if (dark) AppearancePreview(
        canvas = Color(0xFF08090D), surface = Color(0xFF12151E), accent = Color(0xFFC8F55A),
        onSurface = Color(0xFFF3F6EE), onAccent = Color(0xFF08090D), muted = Color(0xFFAEB5AA)
    ) else AppearancePreview(
        canvas = Color(0xFFF4F6F1), surface = Color(0xFFFFFFFF), accent = Color(0xFF4D7A27),
        onSurface = Color(0xFF15200F), onAccent = Color.White, muted = Color(0xFF65715E)
    )
}
