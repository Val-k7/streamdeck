package com.androidcontroldeck.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.data.storage.ProfileSerializer
import com.androidcontroldeck.data.storage.ProfileValidator
import com.androidcontroldeck.ui.components.ButtonControl
import com.androidcontroldeck.ui.components.FaderControl
import com.androidcontroldeck.ui.components.ImportProfileDialog
import com.androidcontroldeck.ui.components.KnobControl
import com.androidcontroldeck.ui.components.PadControl
import com.androidcontroldeck.ui.components.ToggleControl
import com.androidcontroldeck.ui.utils.UndoRedoManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
        profile: Profile?,
        onProfileChanged: (Profile) -> Unit,
        onNavigateBack: () -> Unit,
        assetCache: AssetCache,
        onExportProfile: (Profile) -> Unit,
        onImportProfile: (String) -> Unit,
        modifier: Modifier = Modifier
) {
    val editableProfile = remember(profile) { mutableStateOf(profile) }
    val selectedControlId = remember { mutableStateOf<String?>(null) }
    val showImportDialog = remember { mutableStateOf(false) }
    val importJson = remember { mutableStateOf<String?>(null) }
    val validationResult = remember {
        mutableStateOf<com.androidcontroldeck.data.storage.ProfileValidationResult?>(null)
    }
    val importProfilePreview = remember { mutableStateOf<Profile?>(null) }

    // Système undo/redo
    val undoRedoManager = remember { UndoRedoManager<Profile>(maxHistorySize = 50) }
    val validator = remember { ProfileValidator() }

    val context = LocalContext.current
    val importLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                    uri: Uri? ->
                uri?.let {
                    try {
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            val json =
                                    inputStream.bufferedReader().use { reader -> reader.readText() }
                            importJson.value = json
                            // Valider le profil
                            val result = validator.validateJsonSyntax(json)
                            if (result.first) {
                                try {
                                    val previewProfile = ProfileSerializer().import(json)
                                    importProfilePreview.value = previewProfile
                                    validationResult.value = validator.validate(previewProfile)
                                } catch (e: Exception) {
                                    validationResult.value =
                                            com.androidcontroldeck.data.storage
                                                    .ProfileValidationResult(
                                                            isValid = false,
                                                            errors =
                                                                    listOf(
                                                                            com.androidcontroldeck
                                                                                    .data.storage
                                                                                    .ValidationError(
                                                                                            field =
                                                                                                    "profile",
                                                                                            message =
                                                                                                    "Erreur de parsing: ${e.message}"
                                                                                    )
                                                                    )
                                                    )
                                    importProfilePreview.value = null
                                }
                            } else {
                                validationResult.value =
                                        com.androidcontroldeck.data.storage.ProfileValidationResult(
                                                isValid = false,
                                                errors =
                                                        listOf(
                                                                com.androidcontroldeck.data.storage
                                                                        .ValidationError(
                                                                                field = "json",
                                                                                message =
                                                                                        result.second
                                                                                                ?: "JSON invalide"
                                                                        )
                                                        )
                                        )
                            }
                            showImportDialog.value = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

    LaunchedEffect(profile) {
        editableProfile.value = profile
        profile?.let { undoRedoManager.initialize(it) }
    }

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
                                Icon(
                                        Icons.Default.Save,
                                        contentDescription = stringResource(R.string.editor_back)
                                )
                            }
                        },
                        actions = {
                            // Undo/Redo
                            IconButton(
                                    onClick = {
                                        undoRedoManager.undo(workingProfile)?.let {
                                            editableProfile.value = it
                                            onProfileChanged(it)
                                        }
                                    },
                                    enabled = undoRedoManager.canUndo()
                            ) { Icon(Icons.Default.Undo, contentDescription = "Annuler") }
                            IconButton(
                                    onClick = {
                                        undoRedoManager.redo(workingProfile)?.let {
                                            editableProfile.value = it
                                            onProfileChanged(it)
                                        }
                                    },
                                    enabled = undoRedoManager.canRedo()
                            ) { Icon(Icons.Default.Redo, contentDescription = "Refaire") }

                            // Menu Import/Export/Backup
                            val showImportExportMenu = remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showImportExportMenu.value = true }) {
                                    Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Plus d'options"
                                    )
                                }
                                DropdownMenu(
                                        expanded = showImportExportMenu.value,
                                        onDismissRequest = { showImportExportMenu.value = false }
                                ) {
                                    DropdownMenuItem(
                                            text = { Text("Importer un profil") },
                                            onClick = {
                                                importLauncher.launch("application/json")
                                                showImportExportMenu.value = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                        Icons.Default.Upload,
                                                        contentDescription = null
                                                )
                                            }
                                    )
                                    DropdownMenuItem(
                                            text = { Text("Exporter le profil") },
                                            onClick = {
                                                onExportProfile(workingProfile)
                                                showImportExportMenu.value = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                        Icons.Default.Download,
                                                        contentDescription = null
                                                )
                                            }
                                    )
                                }
                            }
                            IconButton(
                                    onClick = {
                                        selectedControlId.value?.let { id ->
                                            undoRedoManager.push(workingProfile)
                                            val duplicate =
                                                    workingProfile.controls.firstOrNull {
                                                        it.id == id
                                                    }
                                            duplicate?.let {
                                                val copy =
                                                        it.copy(
                                                                id = it.id + "_copy",
                                                                row =
                                                                        (it.row + 1) %
                                                                                workingProfile.rows
                                                        )
                                                editableProfile.value =
                                                        workingProfile.copy(
                                                                controls =
                                                                        workingProfile.controls +
                                                                                copy
                                                        )
                                                onProfileChanged(editableProfile.value!!)
                                            }
                                        }
                                    }
                            ) {
                                Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription =
                                                stringResource(R.string.editor_duplicate)
                                )
                            }

                            IconButton(
                                    onClick = {
                                        selectedControlId.value?.let { id ->
                                            undoRedoManager.push(workingProfile)
                                            editableProfile.value =
                                                    workingProfile.copy(
                                                            controls =
                                                                    workingProfile.controls
                                                                            .filterNot {
                                                                                it.id == id
                                                                            }
                                                    )
                                            onProfileChanged(editableProfile.value!!)
                                            selectedControlId.value = null
                                        }
                                    }
                            ) {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.editor_delete)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors()
                )
            },
            floatingActionButton = {
                val newControlLabel = stringResource(R.string.editor_new_control)
                val addLabel = stringResource(R.string.editor_add)
                androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            undoRedoManager.push(workingProfile)
                            val newControl =
                                    Control(
                                            id = "control_${workingProfile.controls.size}",
                                            type = ControlType.BUTTON,
                                            row = 0,
                                            col = 0,
                                            label = newControlLabel
                                    )
                            editableProfile.value =
                                    workingProfile.copy(
                                            controls = workingProfile.controls + newControl
                                    )
                            onProfileChanged(editableProfile.value!!)
                        }
                ) { Icon(Icons.Default.Add, contentDescription = addLabel) }
            }
    ) { padding ->
        // Dialog d'import
        if (showImportDialog.value) {
            ImportProfileDialog(
                    profile = importProfilePreview.value,
                    validationResult = validationResult.value,
                    onImport = {
                        importJson.value?.let { json ->
                            try {
                                onImportProfile(json)
                                showImportDialog.value = false
                                importJson.value = null
                                validationResult.value = null
                                importProfilePreview.value = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onCancel = {
                        showImportDialog.value = false
                        importJson.value = null
                        validationResult.value = null
                        importProfilePreview.value = null
                    },
                    onResolveConflict = { resolution ->
                        when (resolution) {
                            is ProfileRepository.ImportResolution.Replace -> {
                                importJson.value?.let { json ->
                                    try {
                                        onImportProfile(json)
                                        showImportDialog.value = false
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            is ProfileRepository.ImportResolution.Merge -> {
                                // Fusion : ajouter les contrôles importés qui n'existent pas déjà
                                importProfilePreview.value?.let { importedProfile ->
                                    val existingIds = workingProfile.controls.map { it.id }.toSet()
                                    val newControls =
                                            importedProfile.controls.filter {
                                                it.id !in existingIds
                                            }
                                    if (newControls.isNotEmpty()) {
                                        workingProfile =
                                                workingProfile.copy(
                                                        controls =
                                                                workingProfile.controls +
                                                                        newControls
                                                )
                                    }
                                }
                                showImportDialog.value = false
                            }
                            else -> {
                                showImportDialog.value = false
                            }
                        }
                    }
            )
        }

        Column(modifier = modifier.padding(padding)) {
            LazyVerticalGrid(
                    columns = GridCells.Fixed(workingProfile.cols),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                        workingProfile.controls,
                        key = { it.id },
                        span = { GridItemSpan(it.colSpan) }
                ) { control ->
                    val selected = control.id == selectedControlId.value
                    val iconState:
                            androidx.compose.runtime.State<
                                    androidx.compose.ui.graphics.ImageBitmap?> =
                            rememberCachedIcon(control.icon, assetCache)
                    Card(
                            modifier =
                                    Modifier.animateItemPlacement()
                                            .aspectRatio(
                                                    (control.colSpan.toFloat() / control.rowSpan)
                                                            .coerceAtLeast(0.8f)
                                            )
                                            .pointerInput(control.id) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val deltaRows =
                                                            (dragAmount.y / 180f).roundToInt()
                                                    val deltaCols =
                                                            (dragAmount.x / 180f).roundToInt()
                                                    if (deltaRows != 0 || deltaCols != 0) {
                                                        val updated =
                                                                shiftControl(
                                                                        control,
                                                                        deltaRows,
                                                                        deltaCols,
                                                                        workingProfile
                                                                )
                                                        updateControl(
                                                                updated,
                                                                editableProfile.value!!,
                                                                onProfileChanged,
                                                                undoRedoManager
                                                        )
                                                        selectedControlId.value = updated.id
                                                    }
                                                }
                                            }
                                            .testTag("editor_control_${control.id}"),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    if (selected)
                                                            MaterialTheme.colorScheme
                                                                    .secondaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                    )
                    ) {
                        EditorControlContent(
                                control = control,
                                icon = iconState.value,
                                onChange = { updated ->
                                    updateControl(
                                            updated,
                                            editableProfile.value!!,
                                            onProfileChanged,
                                            undoRedoManager
                                    )
                                    selectedControlId.value = updated.id
                                }
                        )
                    }
                }
            }

            val selectedControl =
                    workingProfile.controls.firstOrNull { it.id == selectedControlId.value }
            if (selectedControl != null) {
                PropertyPanel(control = selectedControl) { updated ->
                    updateControl(
                            updated,
                            editableProfile.value!!,
                            onProfileChanged,
                            undoRedoManager
                    )
                    selectedControlId.value = updated.id
                }
            }
        }
    }
}

private fun shiftControl(
        control: Control,
        deltaRows: Int,
        deltaCols: Int,
        profile: Profile
): Control {
    val newRow = (control.row + deltaRows).coerceIn(0, profile.rows - control.rowSpan)
    val newCol = (control.col + deltaCols).coerceIn(0, profile.cols - control.colSpan)
    val updated = control.copy(row = newRow, col = newCol)
    val collision =
            profile.controls.any { other ->
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

private fun updateControl(
        control: Control,
        profile: Profile,
        onProfileChanged: (Profile) -> Unit,
        undoRedoManager: UndoRedoManager<Profile>
) {
    // Sauvegarder l'état actuel avant modification
    undoRedoManager.push(profile)

    val updatedControls = profile.controls.map { if (it.id == control.id) control else it }
    val updatedProfile = profile.copy(controls = updatedControls)
    onProfileChanged(updatedProfile)
}

@Composable
private fun EditorControlContent(
        control: Control,
        icon: ImageBitmap?,
        onChange: (Control) -> Unit
) {
    when (control.type) {
        ControlType.BUTTON ->
                ButtonControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        onClick = { onChange(control.copy(label = control.label)) },
                        onLongClick = {},
                        modifier = Modifier.fillMaxSize()
                )
        ControlType.TOGGLE ->
                ToggleControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        icon = icon,
                        onToggle = {},
                        modifier = Modifier.fillMaxSize()
                )
        ControlType.FADER ->
                FaderControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        minValue = control.minValue,
                        maxValue = control.maxValue,
                        onValueChanged = {},
                        modifier = Modifier.fillMaxSize()
                )
        ControlType.KNOB ->
                KnobControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        minValue = control.minValue,
                        maxValue = control.maxValue,
                        onDelta = {},
                        modifier = Modifier.fillMaxSize()
                )
        ControlType.PAD ->
                PadControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        onClick = {},
                        onLongClick = {},
                        modifier = Modifier.fillMaxSize()
                )
    }
}

@Composable
fun rememberCachedIcon(
        name: String?,
        assetCache: AssetCache
): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, name, assetCache) {
        value = assetCache.loadIcon(name)
    }
}

@Composable
private fun PropertyPanel(control: Control, onChange: (Control) -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                    stringResource(R.string.editor_properties),
                    style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                    value = control.label,
                    onValueChange = { onChange(control.copy(label = it)) },
                    label = { Text(stringResource(R.string.editor_label)) },
                    modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                        onClick = { onChange(control.copy(type = ControlType.BUTTON)) },
                        label = { Text(stringResource(R.string.editor_type_button)) }
                )
                AssistChip(
                        onClick = { onChange(control.copy(type = ControlType.TOGGLE)) },
                        label = { Text(stringResource(R.string.editor_type_toggle)) }
                )
                AssistChip(
                        onClick = { onChange(control.copy(type = ControlType.FADER)) },
                        label = { Text(stringResource(R.string.editor_type_fader)) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                        onClick = { onChange(control.copy(type = ControlType.KNOB)) },
                        label = { Text(stringResource(R.string.editor_type_knob)) }
                )
                AssistChip(
                        onClick = { onChange(control.copy(type = ControlType.PAD)) },
                        label = { Text(stringResource(R.string.editor_type_pad)) }
                )
            }

            Text(text = stringResource(R.string.editor_width, control.colSpan))
            Slider(
                    value = control.colSpan.toFloat(),
                    onValueChange = {
                        onChange(control.copy(colSpan = it.roundToInt().coerceIn(1, 4)))
                    },
                    steps = 3,
                    valueRange = 1f..4f,
                    modifier = Modifier.testTag("width_slider")
            )

            Text(text = stringResource(R.string.editor_height, control.rowSpan))
            Slider(
                    value = control.rowSpan.toFloat(),
                    onValueChange = {
                        onChange(control.copy(rowSpan = it.roundToInt().coerceIn(1, 4)))
                    },
                    steps = 3,
                    valueRange = 1f..4f,
                    modifier = Modifier.testTag("height_slider")
            )
        }
    }
}
