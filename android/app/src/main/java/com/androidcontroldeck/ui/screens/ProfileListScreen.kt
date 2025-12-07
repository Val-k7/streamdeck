package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    currentProfileId: String?,
    onProfileSelected: (Profile) -> Unit,
    onProfileCreated: (Profile) -> Unit,
    onProfileDeleted: (Profile) -> Unit,
    onProfileDuplicated: (Profile) -> Unit,
    onProfileRenamed: (Profile, String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateWizard by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Si le wizard est affiché, montrer uniquement le wizard
    if (showCreateWizard) {
        com.androidcontroldeck.ui.components.ProfileCreationWizard(
            onProfileCreated = { profile ->
                onProfileCreated(profile)
                showCreateWizard = false
            },
            onCancel = { showCreateWizard = false },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profils") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateWizard = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer un profil")
            }
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Aucun profil",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Créez votre premier profil pour commencer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showCreateWizard = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Créer un profil")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = profiles,
                    key = { it.id },
                    contentType = { "profile" }
                ) { profile ->
                    ProfileListItem(
                        profile = profile,
                        isSelected = profile.id == currentProfileId,
                        onSelect = { onProfileSelected(profile) },
                        onDelete = {
                            profileToDelete = profile
                            showDeleteDialog = true
                        },
                        onDuplicate = { onProfileDuplicated(profile) },
                        onRename = { newName ->
                            onProfileRenamed(profile, newName)
                        }
                    )
                }
            }
        }
    }

    // Dialog de confirmation de suppression
    if (showDeleteDialog && profileToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                profileToDelete = null
            },
            title = { Text("Supprimer le profil") },
            text = { Text("Êtes-vous sûr de vouloir supprimer \"${profileToDelete!!.name}\" ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileToDelete?.let { onProfileDeleted(it) }
                        showDeleteDialog = false
                        profileToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    profileToDelete = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileListItem(
    profile: Profile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember(profile.name) { mutableStateOf(profile.name) }

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevation"
    )

    Card(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
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
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${profile.rows}×${profile.cols} • ${profile.controls.size} contrôles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Profil actuel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Renommer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDuplicate) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Dupliquer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Dialog de renommage
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renommer le profil") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom du profil") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRename(newName)
                            showRenameDialog = false
                        }
                    },
                    enabled = newName.isNotBlank() && newName != profile.name
                ) {
                    Text("Renommer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    newName = profile.name
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Réinitialiser le nom quand le dialog se ferme
    LaunchedEffect(showRenameDialog) {
        if (!showRenameDialog) {
            newName = profile.name
        }
    }
}

