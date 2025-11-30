package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Duplicate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.ui.components.ButtonControl
import com.androidcontroldeck.ui.components.FaderControl
import com.androidcontroldeck.ui.components.KnobControl
import com.androidcontroldeck.ui.components.PadControl
import com.androidcontroldeck.ui.components.ToggleControl
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    profile: Profile?,
    onProfileChanged: (Profile) -> Unit,
    onNavigateBack: () -> Unit,
    assetCache: AssetCache,
    modifier: Modifier = Modifier
) {
    val editableProfile = remember(profile) { mutableStateOf(profile) }
    val selectedControlId = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile) { editableProfile.value = profile }

    if (editableProfile.value == null) {
        Text(stringResource(R.string.editor_no_profile), modifier = modifier.padding(16.dp))
        return
    }

    val workingProfile = editableProfile.value!!
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title), maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.editor_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedControlId.value?.let { id ->
                            val duplicate = workingProfile.controls.firstOrNull { it.id == id }
                            duplicate?.let {
                                val copy = it.copy(id = it.id + "_copy", row = (it.row + 1) % workingProfile.rows)
                                editableProfile.value = workingProfile.copy(controls = workingProfile.controls + copy)
                                onProfileChanged(editableProfile.value!!)
                            }
                        }
                    }) { Icon(Icons.Default.Duplicate, contentDescription = stringResource(R.string.editor_duplicate)) }

                    IconButton(onClick = {
                        selectedControlId.value?.let { id ->
                            editableProfile.value = workingProfile.copy(controls = workingProfile.controls.filterNot { it.id == id })
                            onProfileChanged(editableProfile.value!!)
                        }
                    }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.editor_delete)) }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            IconButton(onClick = {
                val newControl = Control(
                    id = "control_${workingProfile.controls.size}",
                    type = ControlType.BUTTON,
                    row = 0,
                    col = 0,
                    label = stringResource(R.string.editor_new_control)
                )
                editableProfile.value = workingProfile.copy(controls = workingProfile.controls + newControl)
                onProfileChanged(editableProfile.value!!)
            }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.editor_add)) }
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(workingProfile.cols),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workingProfile.controls, key = { it.id }, span = { GridItemSpan(it.colSpan) }) { control ->
                    val selected = control.id == selectedControlId.value
                    val iconState = rememberCachedIcon(control.icon, assetCache)
                    Card(
                        modifier = Modifier
                            .animateItemPlacement()
                            .aspectRatio((control.colSpan.toFloat() / control.rowSpan).coerceAtLeast(0.8f))
                            .pointerInput(control.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaRows = (dragAmount.y / 180f).roundToInt()
                                    val deltaCols = (dragAmount.x / 180f).roundToInt()
                                    if (deltaRows != 0 || deltaCols != 0) {
                                        val updated = shiftControl(control, deltaRows, deltaCols, workingProfile)
                                        updateControl(updated, editableProfile.value!!, onProfileChanged)
                                        selectedControlId.value = updated.id
                                    }
                                }
                            }
                            .testTag("editor_control_${control.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        EditorControlContent(control = control, icon = iconState.value, onChange = { updated ->
                            updateControl(updated, editableProfile.value!!, onProfileChanged)
                            selectedControlId.value = updated.id
                        })
                    }
                }
            }

            val selectedControl = workingProfile.controls.firstOrNull { it.id == selectedControlId.value }
            if (selectedControl != null) {
                PropertyPanel(control = selectedControl) { updated ->
                    updateControl(updated, editableProfile.value!!, onProfileChanged)
                    selectedControlId.value = updated.id
                }
            }
        }
    }
}

private fun shiftControl(control: Control, deltaRows: Int, deltaCols: Int, profile: Profile): Control {
    val newRow = (control.row + deltaRows).coerceIn(0, profile.rows - control.rowSpan)
    val newCol = (control.col + deltaCols).coerceIn(0, profile.cols - control.colSpan)
    val updated = control.copy(row = newRow, col = newCol)
    val collision = profile.controls.any { other ->
        other.id != control.id && rectanglesIntersect(updated, other)
    }
    return if (collision) control else updated
}

private fun rectanglesIntersect(a: Control, b: Control): Boolean {
    val aEndRow = a.row + a.rowSpan
    val aEndCol = a.col + a.colSpan
    val bEndRow = b.row + b.rowSpan
    val bEndCol = b.col + b.colSpan
    return a.row < bEndRow && aEndRow > b.row && a.col < bEndCol && aEndCol > b.col
}

private fun updateControl(control: Control, profile: Profile, onProfileChanged: (Profile) -> Unit) {
    val updatedControls = profile.controls.map { if (it.id == control.id) control else it }
    onProfileChanged(profile.copy(controls = updatedControls))
}

@Composable
private fun EditorControlContent(control: Control, icon: ImageBitmap?, onChange: (Control) -> Unit) {
    when (control.type) {
        ControlType.BUTTON -> ButtonControl(
            label = control.label,
            colorHex = control.colorHex,
            icon = icon,
            onTap = { onChange(control.copy(label = control.label)) },
            onLongPress = { },
            modifier = Modifier.fillMaxSize()
        )

        ControlType.TOGGLE -> ToggleControl(
            label = control.label,
            colorHex = control.colorHex,
            icon = icon,
            onToggle = { },
            modifier = Modifier.fillMaxSize()
        )

        ControlType.FADER -> FaderControl(
            label = control.label,
            colorHex = control.colorHex,
            minValue = control.minValue,
            maxValue = control.maxValue,
            onValueChanged = { },
            modifier = Modifier.fillMaxSize()
        )

        ControlType.KNOB -> KnobControl(
            label = control.label,
            colorHex = control.colorHex,
            minValue = control.minValue,
            maxValue = control.maxValue,
            onDelta = { },
            modifier = Modifier.fillMaxSize()
        )

        ControlType.PAD -> PadControl(
            label = control.label,
            colorHex = control.colorHex,
            onPress = { },
            onLongPress = { },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun rememberCachedIcon(name: String?, assetCache: AssetCache): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, name, assetCache) {
        value = assetCache.loadIcon(name)
    }
}

@Composable
private fun PropertyPanel(control: Control, onChange: (Control) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.editor_properties), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = control.label,
                onValueChange = { onChange(control.copy(label = it)) },
                label = { Text(stringResource(R.string.editor_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = { onChange(control.copy(type = ControlType.BUTTON)) }, label = { Text(stringResource(R.string.editor_type_button)) })
                AssistChip(onClick = { onChange(control.copy(type = ControlType.TOGGLE)) }, label = { Text(stringResource(R.string.editor_type_toggle)) })
                AssistChip(onClick = { onChange(control.copy(type = ControlType.FADER)) }, label = { Text(stringResource(R.string.editor_type_fader)) })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = { onChange(control.copy(type = ControlType.KNOB)) }, label = { Text(stringResource(R.string.editor_type_knob)) })
                AssistChip(onClick = { onChange(control.copy(type = ControlType.PAD)) }, label = { Text(stringResource(R.string.editor_type_pad)) })
            }

            Text(text = stringResource(R.string.editor_width, control.colSpan))
            Slider(
                value = control.colSpan.toFloat(),
                onValueChange = { onChange(control.copy(colSpan = it.roundToInt().coerceIn(1, 3))) },
                steps = 2,
                valueRange = 1f..3f,
                modifier = Modifier.testTag("width_slider")
            )

            Text(text = stringResource(R.string.editor_height, control.rowSpan))
            Slider(
                value = control.rowSpan.toFloat(),
                onValueChange = { onChange(control.copy(rowSpan = it.roundToInt().coerceIn(1, 3))) },
                steps = 2,
                valueRange = 1f..3f,
                modifier = Modifier.testTag("height_slider")
            )
        }
    }
}
