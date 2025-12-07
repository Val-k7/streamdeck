package com.androidcontroldeck.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Animations pour les transitions et interactions
 */

// Durées d'animation standards
object AnimationDurations {
    val Short = 150
    val Medium = 300
    val Long = 500
}

// Courbes d'animation
object AnimationCurves {
    val Standard = FastOutSlowInEasing
    val Decelerate = LinearOutSlowInEasing
    val Accelerate = FastOutLinearInEasing
    val Elastic = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Modifier pour animation de scale au clic
 */
@Composable
fun Modifier.animatedClick(
    onClick: () -> Unit,
    scale: Float = 0.95f,
    duration: Int = AnimationDurations.Short
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleValue by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationCurves.Standard
        ),
        label = "click_scale"
    )

    return this
        .scale(scaleValue)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Animation de fade in/out
 */
@Composable
fun AnimatedVisibilityWithFade(
    visible: Boolean,
    duration: Int = AnimationDurations.Medium,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(duration, easing = AnimationCurves.Standard)
        ) + expandVertically(
            animationSpec = tween(duration, easing = AnimationCurves.Standard)
        ),
        exit = fadeOut(
            animationSpec = tween(duration, easing = AnimationCurves.Standard)
        ) + shrinkVertically(
            animationSpec = tween(duration, easing = AnimationCurves.Standard)
        )
    ) {
        content()
    }
}

/**
 * Animation de slide in/out
 */
@Composable
fun AnimatedVisibilityWithSlide(
    visible: Boolean,
    slideDirection: SlideDirection = SlideDirection.Up,
    duration: Int = AnimationDurations.Medium,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> if (slideDirection == SlideDirection.Up) fullHeight else -fullHeight },
            animationSpec = tween(duration, easing = AnimationCurves.Standard)
        ) + fadeIn(animationSpec = tween(duration)),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> if (slideDirection == SlideDirection.Up) fullHeight else -fullHeight },
            animationSpec = tween(duration, easing = AnimationCurves.Standard)
        ) + fadeOut(animationSpec = tween(duration))
    ) {
        content()
    }
}

enum class SlideDirection {
    Up, Down
}

/**
 * Animation de bounce pour les confirmations
 */
@Composable
fun Modifier.bounceAnimation(
    trigger: Boolean,
    scale: Float = 1.2f
): Modifier {
    val scaleValue by animateFloatAsState(
        targetValue = if (trigger) scale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce"
    )

    LaunchedEffect(trigger) {
        if (trigger) {
            // Reset après l'animation
            kotlinx.coroutines.delay(300)
        }
    }

    return this.scale(scaleValue)
}





