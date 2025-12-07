package com.androidcontroldeck.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Utilitaires pour optimiser les recompositions Compose
 */

/**
 * Cache pour les valeurs calculées coûteuses
 */
@Composable
fun <T> rememberExpensiveCalculation(
    key: Any?,
    calculation: () -> T
): T {
    return remember(key) {
        calculation()
    }
}

/**
 * État dérivé optimisé pour éviter les recalculs inutiles
 */
@Composable
fun <T, R> rememberDerivedState(
    input: T,
    transform: (T) -> R
): R {
    val derivedState = remember {
        derivedStateOf { transform(input) }
    }
    return derivedState.value
}

/**
 * Debounce pour les valeurs qui changent fréquemment
 */
class DebouncedState<T>(
    initialValue: T,
    private val delayMs: Long = 300
) {
    private var _value by mutableStateOf(initialValue)
    val value: T
        get() = _value

    fun update(newValue: T) {
        _value = newValue
        // Debounce logic would be implemented with coroutines
    }
}

/**
 * CompositionLocal pour le cache d'icônes
 */
val LocalIconCache = staticCompositionLocalOf<Map<String, Any>> { emptyMap() }

/**
 * Cache d'icônes pour éviter les recalculs
 */
@Composable
fun rememberIconCache(): Map<String, Any> {
    return remember {
        mutableMapOf<String, Any>()
    }
}


