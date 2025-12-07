package com.androidcontroldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.ProfileSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Dialog d'export de profil avec options de format et partage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportProfileDialog(
    profile: Profile?,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showDialog || profile == null) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var exportFormat by remember { mutableStateOf<ExportFormat>(ExportFormat.JSON) }
    var includeMetadata by remember { mutableStateOf(true) }
    var minifyJson by remember { mutableStateOf(false) }
    var customFileName by remember { mutableStateOf(profile.name.lowercase().replace(" ", "_")) }

    val json = Json {
        prettyPrint = !minifyJson
        ignoreUnknownKeys = true
    }

    val exportedContent = remember(profile, exportFormat, includeMetadata, minifyJson) {
        when (exportFormat) {
            ExportFormat.JSON -> {
                val serializer = ProfileSerializer()
                try {
                    json.encodeToString(serializer.serialize(profile, includeMetadata))
                } catch (e: Exception) {
                    "Erreur lors de l'export: ${e.message}"
                }
            }
            ExportFormat.JSON_MINIFIED -> {
                val serializer = ProfileSerializer()
                try {
                    Json { ignoreUnknownKeys = true }.encodeToString(serializer.serialize(profile, includeMetadata))
                } catch (e: Exception) {
                    "Erreur lors de l'export: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.export_profile_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Options d'export
                Text(
                    text = "Options d'export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                // Format d'export
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = exportFormat == ExportFormat.JSON,
                            onClick = {
                                exportFormat = ExportFormat.JSON
                                minifyJson = false
                            },
                            label = { Text("JSON Formaté") }
                        )
                        FilterChip(
                            selected = exportFormat == ExportFormat.JSON_MINIFIED,
                            onClick = {
                                exportFormat = ExportFormat.JSON_MINIFIED
                                minifyJson = true
                            },
                            label = { Text("JSON Minifié") }
                        )
                    }
                }

                // Nom de fichier
                OutlinedTextField(
                    value = customFileName,
                    onValueChange = { customFileName = it },
                    label = { Text(stringResource(R.string.export_profile_dialog_filename_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${customFileName}.json") }
                )

                // Options supplémentaires
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeMetadata,
                        onCheckedChange = { includeMetadata = it }
                    )
                    Text(
                        text = stringResource(R.string.export_profile_dialog_include_metadata),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider()

                // Aperçu
                Text(
                    text = stringResource(R.string.export_profile_dialog_preview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    SelectionContainer {
                        Text(
                            text = exportedContent.take(500) + if (exportedContent.length > 500) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }

                if (exportedContent.length > 500) {
                    Text(
                        text = "Total: ${exportedContent.length} caractères",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedContent))
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.export_profile_dialog_copy))
                }
                Button(
                    onClick = {
                        onShare(exportedContent)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.export_profile_dialog_share))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.export_profile_dialog_cancel))
            }
        }
    )
}

enum class ExportFormat {
    JSON,
    JSON_MINIFIED
}





