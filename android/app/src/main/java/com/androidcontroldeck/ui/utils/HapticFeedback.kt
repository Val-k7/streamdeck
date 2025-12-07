package com.androidcontroldeck.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Types de feedback haptique
 */
enum class HapticFeedbackType {
    LIGHT,      // Légère vibration (tap)
    MEDIUM,     // Vibration moyenne (long press)
    HEAVY,      // Vibration forte (confirmation)
    DOUBLE,     // Double vibration (toggle)
    SELECTION   // Vibration de sélection (scroll)
}

/**
 * Gestionnaire de feedback haptique
 */
class HapticFeedbackManager(private val vibrator: Vibrator?) {

    fun perform(type: HapticFeedbackType) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        val duration = when (type) {
            HapticFeedbackType.LIGHT -> 10L
            HapticFeedbackType.MEDIUM -> 50L
            HapticFeedbackType.HEAVY -> 100L
            HapticFeedbackType.DOUBLE -> 20L
            HapticFeedbackType.SELECTION -> 5L
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    HapticFeedbackType.DOUBLE -> {
                        // Double vibration
                        val timings = longArrayOf(0, 20, 40, 20)
                        val amplitudes = intArrayOf(0, 50, 0, 50)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    HapticFeedbackType.HEAVY -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    else -> {
                        val amplitude = when (type) {
                            HapticFeedbackType.LIGHT -> 30
                            HapticFeedbackType.MEDIUM -> 100
                            HapticFeedbackType.SELECTION -> 20
                            else -> 50
                        }
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
}

/**
 * Obtient le gestionnaire de feedback haptique pour le contexte actuel
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    return remember { HapticFeedbackManager(vibrator) }
}

/**
 * Modifier pour ajouter le feedback haptique au clic
 */
@Composable
fun Modifier.hapticClick(
    hapticFeedback: HapticFeedbackManager,
    type: HapticFeedbackType = HapticFeedbackType.LIGHT,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    LaunchedEffect(isPressed) {
        if (isPressed) {
            hapticFeedback.perform(type)
        }
    }

    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

