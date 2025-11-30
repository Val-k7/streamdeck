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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.controldeck.ui.components.AccessibleButton
import com.example.controldeck.ui.components.AccessiblePad
import com.example.controldeck.ui.components.AccessibleSlider
import com.androidcontroldeck.R

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
        Text(text = stringResource(R.string.accessible_deck_title), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccessibleButton(
                label = stringResource(R.string.accessible_start),
                onClick = onPrimaryAction,
                modifier = Modifier
                    .focusRequester(focusRequesterButton)
                    .focusOrder(focusRequesterButton) { down = focusRequesterSlider }
                    .semantics { stateDescription = stringResource(R.string.accessible_primary_button_desc) }
                    .onKeyEvent {
                        if (it.key == Key.DirectionCenter) {
                            onPrimaryAction(); true
                        } else false
                    }
            )
            AccessibleButton(
                label = stringResource(R.string.accessible_stop),
                onClick = onSecondaryAction,
                modifier = Modifier
                    .focusRequester(focusRequesterPad)
                    .focusOrder(focusRequesterPad) { right = focusRequesterSlider }
                    .semantics { stateDescription = stringResource(R.string.accessible_secondary_button_desc) }
            )
        }
        AccessibleSlider(
            value = sliderValue.floatValue,
            onValueChange = { sliderValue.floatValue = it },
            label = stringResource(R.string.accessible_volume),
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
                .semantics { stateDescription = stringResource(R.string.accessible_scene_pad_desc) }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                AccessiblePad(
                    title = stringResource(R.string.accessible_scene_one_title),
                    description = stringResource(R.string.accessible_scene_one_desc),
                    onTap = onPrimaryAction,
                    modifier = Modifier.size(width = 140.dp, height = 120.dp)
                )
                AccessiblePad(
                    title = stringResource(R.string.accessible_scene_two_title),
                    description = stringResource(R.string.accessible_scene_two_desc),
                    onTap = onSecondaryAction,
                    modifier = Modifier.size(width = 140.dp, height = 120.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.accessible_controls_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}
