package com.finanza.next.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppExperience(val id: String, val label: String, val description: String) {
    NEXT("next", "Next", "Visual atual, direto e leve"),
    FINANZA("finanza", "Finanza", "Visual completo inspirado no app web");

    companion object {
        fun fromId(id: String?): AppExperience = entries.firstOrNull { it.id == id } ?: NEXT
    }
}

@Immutable
data class AppExperienceTokens(
    val experience: AppExperience,
    val cardRadius: Dp,
    val sheetRadius: Dp,
    val navRadius: Dp,
    val navHeight: Dp,
    val navHorizontalPadding: Dp,
    val navSelectedRadius: Dp,
    val navSelectedLight: Color,
    val navSelectedDark: Color,
    val glassLightAlpha: Float,
    val glassDarkAlpha: Float,
    val quickPanel: Color,
    val quickBorder: Color,
    val quickSecondaryAction: Color,
    val quickPrimaryAction: Color,
    val showNavLabels: Boolean,
    val denseLists: Boolean
)

val NextTokens = AppExperienceTokens(
    experience = AppExperience.NEXT,
    cardRadius = 18.dp,
    sheetRadius = 24.dp,
    navRadius = 23.dp,
    navHeight = 62.dp,
    navHorizontalPadding = 10.dp,
    navSelectedRadius = 18.dp,
    navSelectedLight = HeroCard.copy(alpha = 0.92f),
    navSelectedDark = Color(0xFF34343A).copy(alpha = 0.80f),
    glassLightAlpha = 0.82f,
    glassDarkAlpha = 0.78f,
    quickPanel = Color(0xD9212228),
    quickBorder = Color.White.copy(alpha = 0.14f),
    quickSecondaryAction = Color.White.copy(alpha = 0.15f),
    quickPrimaryAction = AccentBlue,
    showNavLabels = true,
    denseLists = true
)

val FinanzaTokens = AppExperienceTokens(
    experience = AppExperience.FINANZA,
    cardRadius = 20.dp,
    sheetRadius = 24.dp,
    navRadius = 20.dp,
    navHeight = 64.dp,
    navHorizontalPadding = 0.dp,
    navSelectedRadius = 14.dp,
    navSelectedLight = Color(0xFF101113).copy(alpha = 0.88f),
    navSelectedDark = Color.White.copy(alpha = 0.18f),
    glassLightAlpha = 0.88f,
    glassDarkAlpha = 0.88f,
    quickPanel = Color(0xE6141414),
    quickBorder = Color.White.copy(alpha = 0.18f),
    quickSecondaryAction = Color.White.copy(alpha = 0.16f),
    quickPrimaryAction = FinanzaAccent,
    showNavLabels = true,
    denseLists = false
)

val LocalAppExperience = staticCompositionLocalOf { AppExperience.NEXT }
val LocalAppExperienceTokens = staticCompositionLocalOf { NextTokens }
