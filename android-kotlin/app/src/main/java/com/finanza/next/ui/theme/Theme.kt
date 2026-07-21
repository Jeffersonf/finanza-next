package com.finanza.next.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = HeroCard,
    onPrimary = Color.White,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = TextSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = HeroCard,
    onTertiary = Color.White,
    tertiaryContainer = SurfaceVariantLight,
    onTertiaryContainer = TextPrimaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    inverseSurface = HeroCard,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = Color.White,
    outline = Color(0xFFAEAEB2),
    outlineVariant = DividerLight,
    scrim = Color.Black,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedBg,
    onErrorContainer = DangerRed
)

private val DarkColors = darkColorScheme(
    primary = TextPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = TextSecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TextPrimaryDark,
    onTertiary = Color.Black,
    tertiaryContainer = SurfaceVariantDark,
    onTertiaryContainer = TextPrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    inverseSurface = HeroCard,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = TextPrimaryDark,
    outline = Color(0xFF636366),
    outlineVariant = DividerDark,
    scrim = Color.Black,
    error = DangerRed,
    onError = Color.Black,
    errorContainer = DangerRedBg,
    onErrorContainer = DangerRed
)

private val FinanzaLightColors = lightColorScheme(
    primary = FinanzaAccentLight,
    onPrimary = Color.White,
    primaryContainer = FinanzaAccentSoft,
    onPrimaryContainer = FinanzaInkLight,
    secondary = FinanzaTealLight,
    onSecondary = Color.White,
    secondaryContainer = FinanzaSurfaceVariantLight,
    onSecondaryContainer = FinanzaInkLight,
    tertiary = FinanzaPurpleLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEAE4FF),
    onTertiaryContainer = FinanzaInkLight,
    background = FinanzaBackgroundLight,
    surface = FinanzaSurfaceLight,
    onBackground = FinanzaInkLight,
    onSurface = FinanzaInkLight,
    surfaceVariant = FinanzaSurfaceVariantLight,
    onSurfaceVariant = FinanzaMutedLight,
    inverseSurface = FinanzaInkLight,
    inverseOnSurface = FinanzaInkDark,
    inversePrimary = Color.White,
    outline = Color(0xFFA7A7A7),
    outlineVariant = FinanzaDividerLight,
    scrim = Color.Black,
    error = FinanzaDangerLight,
    onError = Color.White,
    errorContainer = FinanzaDangerLight.copy(alpha = 0.14f),
    onErrorContainer = FinanzaDangerLight
)

private val FinanzaDarkColors = darkColorScheme(
    primary = FinanzaAccent,
    onPrimary = Color(0xFF10150A),
    primaryContainer = FinanzaAccentSoftDark,
    onPrimaryContainer = FinanzaAccent,
    secondary = FinanzaMutedDark,
    onSecondary = FinanzaBackgroundDark,
    secondaryContainer = FinanzaSurfaceVariantDark,
    onSecondaryContainer = FinanzaInkDark,
    tertiary = FinanzaTeal,
    onTertiary = Color(0xFF071611),
    tertiaryContainer = FinanzaSurfaceVariantDark,
    onTertiaryContainer = FinanzaTeal,
    background = FinanzaBackgroundDark,
    surface = FinanzaSurfaceDark,
    onBackground = FinanzaInkDark,
    onSurface = FinanzaInkDark,
    surfaceVariant = FinanzaSurfaceVariantDark,
    onSurfaceVariant = FinanzaMutedDark,
    inverseSurface = FinanzaSurfaceDark,
    inverseOnSurface = FinanzaInkDark,
    inversePrimary = FinanzaAccent,
    outline = Color(0xFF5D5D5F),
    outlineVariant = FinanzaDividerDark,
    scrim = Color.Black,
    error = FinanzaDanger,
    onError = FinanzaBackgroundDark,
    errorContainer = FinanzaDanger.copy(alpha = 0.16f),
    onErrorContainer = FinanzaDanger
)

@Composable
fun FinanceAppTheme(
    darkTheme: Boolean,
    experience: AppExperience = AppExperience.NEXT,
    content: @Composable () -> Unit
) {
    val colors = when (experience) {
        AppExperience.NEXT -> if (darkTheme) DarkColors else LightColors
        AppExperience.FINANZA -> if (darkTheme) FinanzaDarkColors else FinanzaLightColors
    }
    val tokens = when (experience) {
        AppExperience.NEXT -> NextTokens
        AppExperience.FINANZA -> FinanzaTokens
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(
        LocalAppExperience provides experience,
        LocalAppExperienceTokens provides tokens
    ) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
