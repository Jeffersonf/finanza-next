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

private val FinanzaWebLightColors = lightColorScheme(
    primary = FinanzaWebAccentLight,
    onPrimary = Color.White,
    primaryContainer = FinanzaWebAccentSoft,
    onPrimaryContainer = FinanzaWebInkLight,
    secondary = FinanzaWebTealLight,
    onSecondary = Color.White,
    secondaryContainer = FinanzaWebSurfaceVariantLight,
    onSecondaryContainer = FinanzaWebInkLight,
    tertiary = FinanzaWebPurpleLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEAE4FF),
    onTertiaryContainer = FinanzaWebInkLight,
    background = FinanzaWebBackgroundLight,
    surface = FinanzaWebSurfaceLight,
    onBackground = FinanzaWebInkLight,
    onSurface = FinanzaWebInkLight,
    surfaceVariant = FinanzaWebSurfaceVariantLight,
    onSurfaceVariant = FinanzaWebMutedLight,
    inverseSurface = FinanzaWebInkLight,
    inverseOnSurface = FinanzaWebInkDark,
    inversePrimary = Color.White,
    outline = Color(0xFF92A184),
    outlineVariant = FinanzaWebDividerLight,
    scrim = Color.Black,
    error = FinanzaWebDangerLight,
    onError = Color.White,
    errorContainer = FinanzaWebDangerLight.copy(alpha = 0.14f),
    onErrorContainer = FinanzaWebDangerLight
)

private val FinanzaWebDarkColors = darkColorScheme(
    primary = FinanzaWebAccent,
    onPrimary = FinanzaWebBackgroundDark,
    primaryContainer = FinanzaWebAccentSoftDark,
    onPrimaryContainer = FinanzaWebAccent,
    secondary = FinanzaWebTeal,
    onSecondary = Color(0xFF071611),
    secondaryContainer = FinanzaWebSurfaceVariantDark,
    onSecondaryContainer = FinanzaWebInkDark,
    tertiary = FinanzaWebPurple,
    onTertiary = Color(0xFF161126),
    tertiaryContainer = FinanzaWebSurfaceVariantDark,
    onTertiaryContainer = FinanzaWebPurple,
    background = FinanzaWebBackgroundDark,
    surface = FinanzaWebSurfaceDark,
    onBackground = FinanzaWebInkDark,
    onSurface = FinanzaWebInkDark,
    surfaceVariant = FinanzaWebSurfaceVariantDark,
    onSurfaceVariant = FinanzaWebMutedDark,
    inverseSurface = FinanzaWebSurfaceDark,
    inverseOnSurface = FinanzaWebInkDark,
    inversePrimary = FinanzaWebAccent,
    outline = Color(0xFF5D655B),
    outlineVariant = FinanzaWebDividerDark,
    scrim = Color.Black,
    error = FinanzaWebDanger,
    onError = FinanzaWebBackgroundDark,
    errorContainer = FinanzaWebDanger.copy(alpha = 0.16f),
    onErrorContainer = FinanzaWebDanger
)

@Composable
fun FinanceAppTheme(
    darkTheme: Boolean,
    experience: AppExperience = AppExperience.NEXT,
    edgeToEdge: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when (experience) {
        AppExperience.NEXT -> if (darkTheme) DarkColors else LightColors
        AppExperience.FINANZA -> if (darkTheme) FinanzaDarkColors else FinanzaLightColors
        AppExperience.WEB -> if (darkTheme) FinanzaWebDarkColors else FinanzaWebLightColors
    }
    val tokens = when (experience) {
        AppExperience.NEXT -> NextTokens
        AppExperience.FINANZA -> FinanzaTokens
        AppExperience.WEB -> FinanzaWebTokens
    }
    val typography = when (experience) {
        AppExperience.NEXT -> AppTypography
        AppExperience.FINANZA -> FinanzaTypography
        AppExperience.WEB -> FinanzaTypography
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, !edgeToEdge)
        if (edgeToEdge) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
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
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
