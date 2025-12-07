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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.ProfileValidationResult
import com.androidcontroldeck.data.storage.ValidationError
import com.androidcontroldeck.data.storage.ValidationWarning

/**
 * Dialog d'import de profil avec validation et aperçu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProfileDialog(
    profile: Profile?,
    validationResult: ProfileValidationResult?,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onResolveConflict: (com.androidcontroldeck.data.repository.ProfileRepository.ImportResolution) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Importer un profil",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // Aperçu du profil
                if (profile != null) {
                    ProfilePreviewCard(profile = profile)
                }

                // Résultats de validation
                if (validationResult != null) {
                    ValidationResultsCard(validationResult = validationResult)
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }

                    if (validationResult?.isValid == true || validationResult == null) {
                        Button(
                            onClick = onImport,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Importer")
                        }
                    } else {
                        // Options de résolution de conflit
                        TextButton(
                            onClick = { onResolveConflict(com.androidcontroldeck.data.repository.ProfileRepository.ImportResolution.Replace) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remplacer")
                        }
                        TextButton(
                            onClick = { onResolveConflict(com.androidcontroldeck.data.repository.ProfileRepository.ImportResolution.Merge) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fusionner")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePreviewCard(profile: Profile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Aperçu du profil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Nom: ${profile.name}")
            Text("ID: ${profile.id}")
            Text("Dimensions: ${profile.rows} × ${profile.cols}")
            Text("Contrôles: ${profile.controls.size}")
            Text("Version: ${profile.version}")
        }
    }
}

@Composable
private fun ValidationResultsCard(validationResult: ProfileValidationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (validationResult.isValid) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (validationResult.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (validationResult.isValid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = if (validationResult.isValid) {
                        "Profil valide"
                    } else {
                        "Erreurs de validation"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (validationResult.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            // Erreurs
            if (validationResult.errors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Erreurs (${validationResult.errors.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    validationResult.errors.forEach { error ->
                        ValidationItem(
                            message = "${error.field}: ${error.message}",
                            isError = true
                        )
                    }
                }
            }

            // Avertissements
            if (validationResult.warnings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Avertissements (${validationResult.warnings.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    validationResult.warnings.forEach { warning ->
                        ValidationItem(
                            message = "${warning.field}: ${warning.message}",
                            isError = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationItem(message: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isError) Icons.Default.Error else Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}





