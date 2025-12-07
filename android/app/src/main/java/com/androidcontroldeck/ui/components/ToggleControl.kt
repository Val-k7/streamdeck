package com.androidcontroldeck.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.foundation.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.network.model.ActionFeedback

@Composable
fun ToggleControl(
    label: String,
    colorHex: String?,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageBitmap? = null,
    feedback: ActionFeedback? = null,
) {
    val haptic = LocalHapticFeedback.current
    val isOn = remember { mutableStateOf(false) }
    val container = if (isOn.value) {
        colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

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
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = label,
                        modifier = Modifier,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isOn.value,
                    onCheckedChange = {
                        isOn.value = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle(it)
                    }
                )
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
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
