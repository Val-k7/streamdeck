package com.example.controldeck.model

import androidx.compose.runtime.Immutable

/**
 * User-selected accessibility preferences that can be persisted alongside other settings.
 */
@Immutable
data class AccessibilityPreferences(
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val largeTextEnabled: Boolean = false
)
