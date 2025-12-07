package com.androidcontroldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Action
import com.androidcontroldeck.data.model.ActionType

/**
 * Éditeur d'actions avec sélecteur par catégories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionEditor(
    currentAction: Action?,
    onActionChanged: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<ActionCategory?>(currentAction?.let { getCategoryForAction(it.type) }) }
    var expandedCategory by remember { mutableStateOf<ActionCategory?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuration de l'action",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            // Catégories d'actions
            ActionCategory.values().forEach { category ->
                ActionCategoryCard(
                    category = category,
                    isExpanded = expandedCategory == category,
                    onExpandedChange = { expanded ->
                        expandedCategory = if (expanded) category else null
                    },
                    selectedActionType = currentAction?.type,
                    onActionTypeSelected = { actionType ->
                        onActionChanged(Action(type = actionType, payload = getDefaultPayload(actionType)))
                        selectedCategory = category
                        expandedCategory = null
                    }
                )
            }

            // Éditeur de payload si une action est sélectionnée
            currentAction?.let { action ->
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ActionPayloadEditor(
                    action = action,
                    onPayloadChanged = { payload ->
                        onActionChanged(action.copy(payload = payload))
                    }
                )
            }
        }
    }
}

/**
 * Catégories d'actions
 */
enum class ActionCategory(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val actionTypes: List<ActionType>
) {
    KEYBOARD(
        "Clavier",
        Icons.Default.Keyboard,
        listOf(ActionType.KEYBOARD)
    ),
    AUDIO(
        "Audio",
        Icons.Default.VolumeUp,
        listOf(ActionType.AUDIO)
    ),
    OBS(
        "OBS Studio",
        Icons.Default.VideoCall,
        listOf(ActionType.OBS)
    ),
    PLUGINS(
        "Plugins",
        Icons.Default.Extension,
        listOf(ActionType.CUSTOM)
    ),
    SYSTEM(
        "Système",
        Icons.Default.Settings,
        listOf(ActionType.SCRIPT)
    )
}

/**
 * Carte de catégorie d'action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCategoryCard(
    category: ActionCategory,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedActionType: ActionType?,
    onActionTypeSelected: (ActionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onExpandedChange(!isExpanded) },
        colors = CardDefaults.cardColors(
            containerColor = if (selectedActionType in category.actionTypes) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    category.actionTypes.forEach { actionType ->
                        ActionTypeChip(
                            actionType = actionType,
                            isSelected = selectedActionType == actionType,
                            onClick = { onActionTypeSelected(actionType) }
                        )
                    }

                    // Actions spécifiques par catégorie
                    when (category) {
                        ActionCategory.PLUGINS -> {
                            Text(
                                text = "Plugins disponibles :",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "• discord:mute, discord:deafen",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• spotify:play_pause, spotify:next",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionTypeChip(
    actionType: ActionType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(getActionTypeName(actionType)) }
    )
}

@Composable
private fun ActionPayloadEditor(
    action: Action,
    onPayloadChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.titleSmall
        )

        when (action.type) {
            ActionType.KEYBOARD -> {
                OutlinedTextField(
                    value = action.payload ?: "",
                    onValueChange = onPayloadChanged,
                    label = { Text("Raccourci clavier (ex: CTRL+S)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("CTRL+SHIFT+S") }
                )
                Text(
                    text = "Exemples : CTRL+S, ALT+F4, SHIFT+F1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ActionType.AUDIO -> {
                val payloadObj = try {
                    if (action.payload != null) {
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(action.payload)
                    } else {
                        emptyMap()
                    }
                } catch (e: Exception) {
                    emptyMap()
                }

                val actionType = (payloadObj["action"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "SET_VOLUME"
                val volumeValue = (payloadObj["volume"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()?.toFloat() ?: 50f
                var volume by remember { mutableStateOf(volumeValue) }

                Text("Type d'action audio : $actionType")
                Text("Volume : ${volume.toInt()}%")
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        onPayloadChanged("""{"action": "SET_VOLUME", "volume": ${it.toInt()}}""")
                    },
                    valueRange = 0f..100f
                )
            }

            ActionType.OBS -> {
                OutlinedTextField(
                    value = action.payload ?: "",
                    onValueChange = onPayloadChanged,
                    label = { Text("Action OBS") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("StartStreaming") },
                    supportingText = {
                        Text("Actions : StartStreaming, StopStreaming, ToggleRecording, SET_SCENE, etc.")
                    }
                )
            }

            ActionType.CUSTOM -> {
                OutlinedTextField(
                    value = action.payload ?: "",
                    onValueChange = onPayloadChanged,
                    label = { Text("Configuration plugin") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("""{"plugin": "discord", "action": "mute"}""") }
                )
            }

            ActionType.SCRIPT -> {
                OutlinedTextField(
                    value = action.payload ?: "",
                    onValueChange = onPayloadChanged,
                    label = { Text("Script ou commande") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        }
    }
}

private fun getCategoryForAction(actionType: ActionType): ActionCategory {
    return ActionCategory.values().firstOrNull { actionType in it.actionTypes }
        ?: ActionCategory.SYSTEM
}

private fun getActionTypeName(actionType: ActionType): String {
    return when (actionType) {
        ActionType.KEYBOARD -> "Raccourci clavier"
        ActionType.AUDIO -> "Contrôle audio"
        ActionType.OBS -> "OBS Studio"
        ActionType.CUSTOM -> "Plugin personnalisé"
        ActionType.SCRIPT -> "Script/Commande"
    }
}

private fun getDefaultPayload(actionType: ActionType): String {
    return when (actionType) {
        ActionType.KEYBOARD -> "CTRL+S"
        ActionType.AUDIO -> """{"action": "SET_VOLUME", "volume": 50}"""
        ActionType.OBS -> "StartStreaming"
        ActionType.CUSTOM -> """{"plugin": "discord", "action": "mute"}"""
        ActionType.SCRIPT -> "echo 'Hello'"
    }
}

