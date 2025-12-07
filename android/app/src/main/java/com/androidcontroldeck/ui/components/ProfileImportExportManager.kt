package com.androidcontroldeck.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.repository.ProfileRepository
import com.androidcontroldeck.data.storage.ProfileBackupManager
import com.androidcontroldeck.data.storage.BackupReason
import com.androidcontroldeck.data.storage.BackupInfo
import com.androidcontroldeck.data.storage.ProfileSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire complet d'import/export de profils avec fonctionnalités avancées
 */
@Composable
fun ProfileImportExportManager(
    profile: Profile,
    profileRepository: ProfileRepository,
    backupManager: ProfileBackupManager,
    onProfileUpdated: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }

    // Launcher pour l'import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val json = inputStream.bufferedReader().use { reader -> reader.readText() }
                        importResult = json
                        showImportDialog = true
                    }
                } catch (e: Exception) {
                    importResult = "Erreur lors de la lecture du fichier: ${e.message}"
                }
            }
        }
    }

    // Launcher pour l'export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val serializer = ProfileSerializer(Json { prettyPrint = true; ignoreUnknownKeys = true })
                    val json = serializer.export(profile)

                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }

                    // Créer une sauvegarde automatique
                    withContext(Dispatchers.IO) {
                        backupManager.createAutoBackup(profile, BackupReason.EXPORT)
                    }
                } catch (e: Exception) {
                    importResult = "Erreur lors de l'export: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Boutons d'action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.import_profile_dialog_title))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showExportDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.export_profile_dialog_title))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { showBackupDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sauvegardes")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val serializer = ProfileSerializer(Json { prettyPrint = true; ignoreUnknownKeys = true })
                        val json = serializer.export(profile)

                        // Copier dans le presse-papiers
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Profile JSON", json)
                        clipboard.setPrimaryClip(clip)

                        importResult = "Profil copié dans le presse-papiers"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copier")
            }
        }

        // Dialog d'import
        if (showImportDialog && importResult != null) {
            ImportProfileDialog(
                profile = null, // Sera chargé depuis importResult
                validationResult = null,
                onImport = {
                    scope.launch {
                        try {
                            val importedProfile = profileRepository.importProfile(importResult!!)
                            onProfileUpdated(importedProfile)
                            showImportDialog = false
                            importResult = null
                        } catch (e: Exception) {
                            importResult = "Erreur lors de l'import: ${e.message}"
                        }
                    }
                },
                onCancel = {
                    showImportDialog = false
                    importResult = null
                },
                onResolveConflict = { resolution ->
                    scope.launch {
                        try {
                            val importedProfile = profileRepository.importProfileWithConflictResolution(importResult!!) { existing, imported ->
                                resolution
                            }
                            onProfileUpdated(importedProfile)
                            showImportDialog = false
                            importResult = null
                        } catch (e: Exception) {
                            importResult = "Erreur lors de l'import: ${e.message}"
                        }
                    }
                }
            )
        }

        // Dialog d'export
        if (showExportDialog) {
            ExportProfileDialog(
                profile = profile,
                showDialog = showExportDialog,
                onDismiss = { showExportDialog = false },
                onShare = { exportedJson ->
                    scope.launch {
                        try {
                            // Créer un fichier temporaire
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val fileName = "${profile.name.replace(" ", "_").lowercase()}_export_$timestamp.json"

                            // Partager le JSON
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportedJson)
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, fileName)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)

                            // Créer une sauvegarde
                            withContext(Dispatchers.IO) {
                                backupManager.createAutoBackup(profile, BackupReason.EXPORT)
                            }

                            showExportDialog = false
                        } catch (e: Exception) {
                            importResult = "Erreur lors de l'export: ${e.message}"
                        }
                    }
                }
            )
        }

        // Dialog de sauvegardes
        if (showBackupDialog) {
            BackupManagerDialog(
                profile = profile,
                backupManager = backupManager,
                onDismiss = { showBackupDialog = false },
                onRestore = { restoredProfile ->
                    onProfileUpdated(restoredProfile)
                    showBackupDialog = false
                }
            )
        }

        // Message de résultat
        importResult?.let { result ->
            if (result.startsWith("Erreur")) {
                Text(
                    text = result,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = result,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Dialog de gestion des sauvegardes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupManagerDialog(
    profile: Profile,
    backupManager: ProfileBackupManager,
    onDismiss: () -> Unit,
    onRestore: (Profile) -> Unit
) {
    val scope = rememberCoroutineScope()
    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            backups = withContext(Dispatchers.IO) {
                backupManager.listBackups()
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestion des sauvegardes") },
        text = {
            if (loading) {
                CircularProgressIndicator()
            } else if (backups.isEmpty()) {
                Text("Aucune sauvegarde disponible")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (backup in backups) {
                        key(backup.filePath) {
                            BackupItem(
                                backup = backup,
                                onRestore = {
                                    scope.launch {
                                        val restored = withContext(Dispatchers.IO) {
                                            backupManager.restoreBackup(backup)
                                        }
                                        restored?.let { onRestore(it) }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            // Supprimer la sauvegarde (à implémenter dans BackupManager)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun BackupItem(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backup.filePath.substringAfterLast("/"),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = backup.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Raison: ${backup.reason.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restaurer")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                }
            }
        }
    }
}

