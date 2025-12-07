package com.androidcontroldeck.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utilitaires pour rendre l'UI responsive selon la taille de l'écran
 */

data class ScreenSize(
    val width: Dp,
    val height: Dp,
    val isTablet: Boolean,
    val isLandscape: Boolean,
    val density: Float
)

/**
 * Détermine la taille de l'écran et ses caractéristiques
 */
@Composable
fun getScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    val isLandscape = width > height
    val isTablet = width >= 600.dp || (isLandscape && width >= 480.dp)
    val density = configuration.densityDpi / 160f

    return ScreenSize(
        width = width,
        height = height,
        isTablet = isTablet,
        isLandscape = isLandscape,
        density = density
    )
}

/**
 * Calcule le nombre de colonnes optimal pour une grille selon la taille de l'écran
 */
@Composable
fun calculateOptimalColumns(
    baseColumns: Int = 4,
    minColumnWidth: Dp = 100.dp
): Int {
    val screenSize = getScreenSize()
    val availableWidth = if (screenSize.isLandscape) screenSize.height else screenSize.width

    // Calculer le nombre de colonnes basé sur la largeur minimale
    val calculatedColumns = (availableWidth / minColumnWidth).toInt().coerceAtLeast(1)

    // Pour les tablettes, augmenter le nombre de colonnes
    val adjustedColumns = if (screenSize.isTablet) {
        (calculatedColumns * 1.5f).toInt().coerceAtMost(12)
    } else {
        calculatedColumns.coerceAtMost(8)
    }

    return adjustedColumns.coerceAtLeast(baseColumns)
}

/**
 * Calcule le padding adaptatif selon la taille de l'écran
 */
@Composable
fun adaptivePadding(
    small: Dp = 8.dp,
    medium: Dp = 16.dp,
    large: Dp = 24.dp
): Dp {
    val screenSize = getScreenSize()
    return when {
        screenSize.isTablet -> large
        screenSize.width > 400.dp -> medium
        else -> small
    }
}

/**
 * Calcule la taille de police adaptative
 */
@Composable
fun adaptiveFontSize(
    small: Float = 12f,
    medium: Float = 14f,
    large: Float = 16f
): Float {
    val screenSize = getScreenSize()
    return when {
        screenSize.isTablet -> large
        screenSize.width > 400.dp -> medium
        else -> small
    }
}

/**
 * Calcule l'espacement adaptatif entre les éléments
 */
@Composable
fun adaptiveSpacing(
    small: Dp = 4.dp,
    medium: Dp = 8.dp,
    large: Dp = 16.dp
): Dp {
    val screenSize = getScreenSize()
    return when {
        screenSize.isTablet -> large
        screenSize.width > 400.dp -> medium
        else -> small
    }
}

/**
 * Détermine si on doit utiliser un layout à deux colonnes (pour tablettes)
 */
@Composable
fun shouldUseTwoColumnLayout(): Boolean {
    val screenSize = getScreenSize()
    return screenSize.isTablet && screenSize.isLandscape
}

/**
 * Calcule la taille d'icône adaptative
 */
@Composable
fun adaptiveIconSize(
    small: Dp = 24.dp,
    medium: Dp = 32.dp,
    large: Dp = 48.dp
): Dp {
    val screenSize = getScreenSize()
    return when {
        screenSize.isTablet -> large
        screenSize.width > 400.dp -> medium
        else -> small
    }
}

/**
 * Calcule le nombre de lignes optimal pour une liste selon la taille de l'écran
 */
@Composable
fun calculateOptimalRows(
    baseRows: Int = 3,
    itemHeight: Dp = 100.dp
): Int {
    val screenSize = getScreenSize()
    val availableHeight = if (screenSize.isLandscape) screenSize.width else screenSize.height

    val calculatedRows = (availableHeight / itemHeight).toInt().coerceAtLeast(1)
    return calculatedRows.coerceAtLeast(baseRows)
}

/**
 * Détermine si on doit afficher un mode compact
 */
@Composable
fun shouldUseCompactMode(): Boolean {
    val screenSize = getScreenSize()
    return screenSize.width < 360.dp || (!screenSize.isTablet && screenSize.isLandscape)
}

/**
 * Calcule la largeur maximale du contenu pour les tablettes
 */
@Composable
fun maxContentWidth(): Dp {
    val screenSize = getScreenSize()
    return if (screenSize.isTablet) {
        1200.dp
    } else {
        Dp.Unspecified
    }
}





