package com.androidcontroldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.templates.ProfileTemplates
import com.androidcontroldeck.ui.utils.adaptivePadding
import com.androidcontroldeck.ui.utils.adaptiveSpacing

/**
 * Assistant de création de profil (wizard) pour guider les nouveaux utilisateurs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCreationWizard(
    onProfileCreated: (Profile) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4

    var profileName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<Profile?>(null) }
    var rows by remember { mutableStateOf(4) }
    var cols by remember { mutableStateOf(4) }
    var useTemplate by remember { mutableStateOf(false) }

    val padding = adaptivePadding()
    val spacing = adaptiveSpacing()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // En-tête avec progression
            WizardHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                onCancel = onCancel
            )

            Spacer(modifier = Modifier.height(spacing))

            // Contenu du wizard selon l'étape
            when (currentStep) {
                0 -> WelcomeStep(
                    onNext = { currentStep++ },
                    modifier = Modifier.weight(1f)
                )
                1 -> TemplateSelectionStep(
                    selectedTemplate = selectedTemplate,
                    onTemplateSelected = { selectedTemplate = it },
                    useTemplate = useTemplate,
                    onUseTemplateChanged = { useTemplate = it },
                    onNext = {
                        if (useTemplate && selectedTemplate != null) {
                            // Créer le profil directement depuis le template
                            val template = selectedTemplate!!
                            val profile = template.copy(
                                id = "profile_${System.currentTimeMillis()}",
                                name = profileName.ifBlank { template.name }
                            )
                            onProfileCreated(profile)
                        } else {
                            currentStep++
                        }
                    },
                    onBack = { currentStep-- },
                    modifier = Modifier.weight(1f)
                )
                2 -> ProfileConfigurationStep(
                    profileName = profileName,
                    onProfileNameChanged = { profileName = it },
                    rows = rows,
                    onRowsChanged = { rows = it },
                    cols = cols,
                    onColsChanged = { cols = it },
                    onNext = { currentStep++ },
                    onBack = { currentStep-- },
                    modifier = Modifier.weight(1f)
                )
                3 -> ReviewStep(
                    profileName = profileName,
                    rows = rows,
                    cols = cols,
                    template = selectedTemplate,
                    onCreate = {
                        val profile = Profile(
                            id = "profile_${System.currentTimeMillis()}",
                            name = profileName.ifBlank { "Nouveau Profil" },
                            rows = rows,
                            cols = cols,
                            controls = selectedTemplate?.controls ?: emptyList()
                        )
                        onProfileCreated(profile)
                    },
                    onBack = { currentStep-- },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WizardHeader(
    currentStep: Int,
    totalSteps: Int,
    onCancel: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.wizard_title),
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.wizard_cancel))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barre de progression
        LinearProgressIndicator(
            progress = (currentStep + 1).toFloat() / totalSteps,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.wizard_step, currentStep + 1, totalSteps),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.wizard_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.wizard_welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.wizard_start))
        }
    }
}

@Composable
private fun TemplateSelectionStep(
    selectedTemplate: Profile?,
    onTemplateSelected: (Profile) -> Unit,
    useTemplate: Boolean,
    onUseTemplateChanged: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.wizard_template_title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.wizard_template_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Option : utiliser un template ou créer un profil vide
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = useTemplate,
                onCheckedChange = onUseTemplateChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.wizard_use_template),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (useTemplate) {
            Spacer(modifier = Modifier.height(16.dp))

            // Liste des templates disponibles
            val context = LocalContext.current
            val templates = ProfileTemplates.getAllTemplates { key ->
                val resourceId = when (key) {
                    "template_streaming_name" -> R.string.template_streaming_name
                    "template_audio_production_name" -> R.string.template_audio_production_name
                    "template_gaming_name" -> R.string.template_gaming_name
                    "template_productivity_name" -> R.string.template_productivity_name
                    "template_btn_start_stream" -> R.string.template_btn_start_stream
                    "template_btn_stop_stream" -> R.string.template_btn_stop_stream
                    "template_btn_record" -> R.string.template_btn_record
                    "template_scene_1" -> R.string.template_scene_1
                    "template_scene_2" -> R.string.template_scene_2
                    "template_scene_3" -> R.string.template_scene_3
                    "template_mic_volume" -> R.string.template_mic_volume
                    "template_mute_mic" -> R.string.template_mute_mic
                    "template_system_volume" -> R.string.template_system_volume
                    "template_master_volume" -> R.string.template_master_volume
                    "template_track_1" -> R.string.template_track_1
                    "template_track_2" -> R.string.template_track_2
                    "template_mic_1" -> R.string.template_mic_1
                    "template_mic_2" -> R.string.template_mic_2
                    "template_mute_all" -> R.string.template_mute_all
                    "template_switch_device" -> R.string.template_switch_device
                    "template_balance" -> R.string.template_balance
                    "template_high" -> R.string.template_high
                    "template_save" -> R.string.template_save
                    "template_load" -> R.string.template_load
                    "template_quick_save" -> R.string.template_quick_save
                    "template_pause" -> R.string.template_pause
                    "template_discord_mute" -> R.string.template_discord_mute
                    "template_discord_deafen" -> R.string.template_discord_deafen
                    "template_media_play" -> R.string.template_media_play
                    "template_media_next" -> R.string.template_media_next
                    "template_game_volume" -> R.string.template_game_volume
                    "template_new" -> R.string.template_new
                    "template_open" -> R.string.template_open
                    "template_print" -> R.string.template_print
                    "template_copy" -> R.string.template_copy
                    "template_paste" -> R.string.template_paste
                    "template_undo" -> R.string.template_undo
                    "template_redo" -> R.string.template_redo
                    "template_screenshot" -> R.string.template_screenshot
                    "template_lock" -> R.string.template_lock
                    "template_mute" -> R.string.template_mute
                    "template_volume" -> R.string.template_volume
                    else -> R.string.app_name
                }
                context.getString(resourceId)
            }
            templates.forEach { template ->
                TemplateCard(
                    template = template,
                    selected = selectedTemplate?.id == template.id,
                    onClick = { onTemplateSelected(template) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons de navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.wizard_back))
            }
            Button(
                onClick = onNext,
                enabled = !useTemplate || selectedTemplate != null
            ) {
                Text(stringResource(R.string.wizard_next))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCard(
    template: Profile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${template.rows}x${template.cols} - ${template.controls.size} controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProfileConfigurationStep(
    profileName: String,
    onProfileNameChanged: (String) -> Unit,
    rows: Int,
    onRowsChanged: (Int) -> Unit,
    cols: Int,
    onColsChanged: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.wizard_config_title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.wizard_config_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nom du profil
        OutlinedTextField(
            value = profileName,
            onValueChange = onProfileNameChanged,
            label = { Text(stringResource(R.string.wizard_profile_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nombre de lignes
        Text(
            text = stringResource(R.string.wizard_rows, rows),
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = rows.toFloat(),
            onValueChange = { onRowsChanged(it.toInt()) },
            valueRange = 2f..8f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nombre de colonnes
        Text(
            text = stringResource(R.string.wizard_cols, cols),
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = cols.toFloat(),
            onValueChange = { onColsChanged(it.toInt()) },
            valueRange = 2f..8f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Aperçu de la grille
        Text(
            text = stringResource(R.string.wizard_preview),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        GridPreview(rows = rows, cols = cols)

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons de navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.wizard_back))
            }
            Button(
                onClick = onNext,
                enabled = profileName.isNotBlank()
            ) {
                Text(stringResource(R.string.wizard_next))
            }
        }
    }
}

@Composable
private fun GridPreview(rows: Int, cols: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            repeat(rows) { row ->
                Row {
                    repeat(cols) { col ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.shapes.small
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    profileName: String,
    rows: Int,
    cols: Int,
    template: Profile?,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.wizard_review_title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.wizard_review_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Résumé du profil
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewItem(
                    label = stringResource(R.string.wizard_profile_name),
                    value = profileName
                )
                ReviewItem(
                    label = stringResource(R.string.wizard_rows),
                    value = rows.toString()
                )
                ReviewItem(
                    label = stringResource(R.string.wizard_cols),
                    value = cols.toString()
                )
                if (template != null) {
                    ReviewItem(
                        label = stringResource(R.string.wizard_template),
                        value = template.name
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boutons de navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.wizard_back))
            }
            Button(onClick = onCreate) {
                Text(stringResource(R.string.wizard_create))
            }
        }
    }
}

@Composable
private fun ReviewItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

