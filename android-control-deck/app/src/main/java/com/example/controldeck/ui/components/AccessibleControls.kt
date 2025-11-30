package com.example.controldeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FocusOutlineColor = Color(0xFF0047AB)
private val HighContrastSurface = Color(0xFF0D0D0D)
private val HighContrastForeground = Color(0xFFF4F4F4)

/**
 * Common modifier applied to every interactive control to respect minimum touch targets
 * and visible focus outlines for d-pad/keyboard navigation.
 */
fun Modifier.accessibleControl(
    label: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    role: Role
): Modifier = composed {
    val focusShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .shadow(elevation = 4.dp, shape = focusShape)
        .border(BorderStroke(2.dp, FocusOutlineColor), focusShape)
        .focusable(interactionSource = interactionSource)
        .semantics {
            contentDescription = label
            this.role = role
            onClick?.let { onClick("${'$'}label activated") { it(); true } }
            onLongClick?.let { onLongClick("${'$'}label long press") { it(); true } }
        }
}

@Composable
fun AccessibleButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    backgroundColor: Color = HighContrastSurface,
    foregroundColor: Color = HighContrastForeground
) {
    Button(
        onClick = onClick,
        modifier = modifier.accessibleControl(
            label = label,
            onClick = onClick,
            onLongClick = onLongPress,
            role = Role.Button
        ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = foregroundColor
        )
    ) {
        Text(text = label, color = foregroundColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AccessibleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    Column(
        modifier = modifier
            .accessibleControl(label = label, onClick = null, role = Role.Slider)
            .background(HighContrastSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, color = HighContrastForeground)
            Text(text = "${'$'}{(value * 100).toInt()}%", color = HighContrastForeground, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AccessiblePad(
    title: String,
    description: String,
    onTap: () -> Unit,
    onHold: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.accessibleControl(
            label = "$title, $description",
            onClick = onTap,
            onLongClick = onHold,
            role = Role.Button
        ),
        color = HighContrastSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = HighContrastForeground, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = HighContrastForeground, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
