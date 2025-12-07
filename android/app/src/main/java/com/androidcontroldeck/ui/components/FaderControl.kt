package com.androidcontroldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.network.model.ActionFeedback

@Composable
fun FaderControl(
    label: String,
    colorHex: String?,
    minValue: Float,
    maxValue: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    feedback: ActionFeedback? = null,
) {
    val value = remember { mutableStateOf(minValue) }
    val color = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.tertiaryContainer

    // Déterminer la couleur de bordure selon le feedback
    val borderColor = when (feedback?.status) {
        ActionFeedback.ActionStatus.SUCCESS -> Color(0xFF4CAF50) // Vert
        ActionFeedback.ActionStatus.ERROR -> Color(0xFFF44336) // Rouge
        ActionFeedback.ActionStatus.PENDING -> Color(0xFFFF9800) // Orange
        null -> Color.Transparent
    }
    val borderWidth = if (feedback != null) 2.dp else 0.dp

    Card(
        modifier = modifier.border(borderWidth, borderColor, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val knobHeight = 12.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            val delta = -(dragAmount / 400f) * (maxValue - minValue)
                            value.value = (value.value + delta).coerceAtLeast(minValue).coerceAtMost(maxValue)
                            onValueChanged(value.value)
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                val ratio = (value.value - minValue) / (maxValue - minValue)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((ratio.coerceAtLeast(0f).coerceAtMost(1f) * 120).dp)
                        .background(color, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .height(knobHeight)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
            }
            Text(
                text = "${label}: ${value.value.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            // Icône de feedback en haut à droite
            if (feedback != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = when (feedback.status) {
                            ActionFeedback.ActionStatus.SUCCESS -> Icons.Default.CheckCircle
                            ActionFeedback.ActionStatus.ERROR -> Icons.Default.Error
                            ActionFeedback.ActionStatus.PENDING -> Icons.Default.HourglassEmpty
                        },
                        contentDescription = when (feedback.status) {
                            ActionFeedback.ActionStatus.SUCCESS -> "Succès"
                            ActionFeedback.ActionStatus.ERROR -> "Erreur: ${feedback.errorMessage}"
                            ActionFeedback.ActionStatus.PENDING -> "En attente"
                        },
                        tint = borderColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
