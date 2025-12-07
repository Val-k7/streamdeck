package com.androidcontroldeck.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.network.model.ActionFeedback

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonControl(
    label: String,
    colorHex: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    feedback: ActionFeedback? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val color = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }

    // Déterminer la couleur de bordure selon le feedback
    val borderColor = when (feedback?.status) {
        ActionFeedback.ActionStatus.SUCCESS -> Color(0xFF4CAF50) // Vert
        ActionFeedback.ActionStatus.ERROR -> Color(0xFFF44336) // Rouge
        ActionFeedback.ActionStatus.PENDING -> Color(0xFFFF9800) // Orange
        null -> Color.Transparent
    }
    val borderWidth = if (feedback != null) 2.dp else 0.dp

    Card(
        modifier = modifier
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // Or define a custom indication
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed.value) {
                color?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                color ?: MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed.value) 2.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
