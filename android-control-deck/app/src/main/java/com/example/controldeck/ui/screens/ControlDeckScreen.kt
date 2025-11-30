package com.example.controldeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.controldeck.ui.components.AccessibleButton
import com.example.controldeck.ui.components.AccessiblePad
import com.example.controldeck.ui.components.AccessibleSlider

@Composable
fun ControlDeckScreen(
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {}
) {
    val sliderValue = remember { mutableFloatStateOf(0.35f) }
    val focusRequesterButton = androidx.compose.ui.focus.FocusRequester()
    val focusRequesterPad = androidx.compose.ui.focus.FocusRequester()
    val focusRequesterSlider = androidx.compose.ui.focus.FocusRequester()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = "Deck accessible", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccessibleButton(
                label = "Démarrer",
                onClick = onPrimaryAction,
                modifier = Modifier
                    .focusRequester(focusRequesterButton)
                    .focusOrder(focusRequesterButton) { down = focusRequesterSlider }
                    .semantics { stateDescription = "Bouton principal" }
                    .onKeyEvent {
                        if (it.key == Key.DirectionCenter) {
                            onPrimaryAction(); true
                        } else false
                    }
            )
            AccessibleButton(
                label = "Arrêter",
                onClick = onSecondaryAction,
                modifier = Modifier
                    .focusRequester(focusRequesterPad)
                    .focusOrder(focusRequesterPad) { right = focusRequesterSlider }
                    .semantics { stateDescription = "Bouton secondaire" }
            )
        }
        AccessibleSlider(
            value = sliderValue.floatValue,
            onValueChange = { sliderValue.floatValue = it },
            label = "Volume",
            modifier = Modifier
                .focusRequester(focusRequesterSlider)
                .focusOrder(focusRequesterSlider) { up = focusRequesterButton; left = focusRequesterPad }
                .onKeyEvent {
                    when (it.key) {
                        Key.DirectionRight -> {
                            sliderValue.floatValue = (sliderValue.floatValue + 0.05f).coerceAtMost(1f)
                            true
                        }
                        Key.DirectionLeft -> {
                            sliderValue.floatValue = (sliderValue.floatValue - 0.05f).coerceAtLeast(0f)
                            true
                        }
                        else -> false
                    }
                }
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = "Pad de scène" }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                AccessiblePad(
                    title = "Scène 1",
                    description = "Caméra + micro",
                    onTap = onPrimaryAction,
                    modifier = Modifier.size(width = 140.dp, height = 120.dp)
                )
                AccessiblePad(
                    title = "Scène 2",
                    description = "Capture d'écran",
                    onTap = onSecondaryAction,
                    modifier = Modifier.size(width = 140.dp, height = 120.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Chaque contrôle possède un label, un rôle TalkBack/VoiceOver et accepte la navigation clavier/d-pad.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}
