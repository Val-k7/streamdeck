package com.androidcontroldeck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.coerceIn

@Composable
fun KnobControl(
    label: String,
    colorHex: String?,
    minValue: Float,
    maxValue: Float,
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.secondaryContainer
    val value = remember { mutableStateOf(minValue) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount.y * -1 + dragAmount.x) / 400f * (maxValue - minValue)
                        value.value = (value.value + delta).coerceIn(minValue, maxValue)
                        onDelta(value.value)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sweep = (value.value - minValue) / (maxValue - minValue) * 270f
                val startAngle = 135f
                drawArc(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    startAngle = startAngle,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                )
            }
            Text(text = "${label}: ${value.value.toInt()}", style = MaterialTheme.typography.labelMedium)
        }
    }
}
