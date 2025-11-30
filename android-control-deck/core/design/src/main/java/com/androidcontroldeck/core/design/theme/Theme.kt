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

@Composable
fun DeckTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> deckDarkColors
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
