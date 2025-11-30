package com.androidcontroldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlin.math.coerceIn

@Composable
fun FaderControl(
    label: String,
    colorHex: String?,
    minValue: Float,
    maxValue: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val value = remember { mutableStateOf(minValue) }
    val color = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.tertiaryContainer

    Card(
        modifier = modifier,
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
                            value.value = (value.value + delta).coerceIn(minValue, maxValue)
                            onValueChanged(value.value)
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                val ratio = (value.value - minValue) / (maxValue - minValue)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((ratio.coerceIn(0f, 1f) * 120).dp)
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
        }
    }
}
