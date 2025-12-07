package com.androidcontroldeck.core.design.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }

/**
 * Mode de thème personnalisé
 */
enum class CustomThemeMode {
    SYSTEM,      // Suit le thème système
    LIGHT,       // Thème clair forcé
    DARK,        // Thème sombre forcé
    CUSTOM       // Thème personnalisé
}

/**
 * Configuration de thème personnalisé
 */
data class CustomThemeColors(
    val primary: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val tertiary: androidx.compose.ui.graphics.Color
)

@Composable
fun DeckTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    customThemeMode: CustomThemeMode = CustomThemeMode.SYSTEM,
    customColors: CustomThemeColors? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Déterminer si on utilise le thème sombre
    val actualDarkTheme = when (customThemeMode) {
        CustomThemeMode.SYSTEM -> useDarkTheme
        CustomThemeMode.LIGHT -> false
        CustomThemeMode.DARK -> true
        CustomThemeMode.CUSTOM -> useDarkTheme // Utiliser le thème système comme base
    }

    val colorScheme = when {
        // Thème personnalisé
        customThemeMode == CustomThemeMode.CUSTOM && customColors != null -> {
            createCustomColorScheme(customColors, actualDarkTheme)
        }
        // Dynamic color (Android 12+)
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (actualDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Thèmes statiques
        actualDarkTheme -> deckDarkColors
        else -> deckLightColors
    }

    val window = (context as? Activity)?.window
    window?.statusBarColor = colorScheme.background.toArgb()

    CompositionLocalProvider(LocalSpacing provides DefaultSpacing) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DeckTypography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}

/**
 * Crée un ColorScheme personnalisé à partir de couleurs personnalisées
 */
@Composable
private fun createCustomColorScheme(
    customColors: CustomThemeColors,
    isDark: Boolean
): androidx.compose.material3.ColorScheme {
    return if (isDark) {
        androidx.compose.material3.darkColorScheme(
            primary = customColors.primary,
            secondary = customColors.secondary,
            tertiary = customColors.tertiary
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = customColors.primary,
            secondary = customColors.secondary,
            tertiary = customColors.tertiary
        )
    }
}
