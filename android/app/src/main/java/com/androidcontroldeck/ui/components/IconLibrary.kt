package com.androidcontroldeck.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R

/**
 * Bibliothèque d'icônes intégrée pour la sélection d'icônes dans l'éditeur
 */

data class IconCategory(
    val name: String,
    val icons: List<IconItem>
)

data class IconItem(
    val name: String,
    val icon: ImageVector,
    val category: String
)

object IconLibrary {
    val categories = listOf(
        IconCategory(
            name = "Contrôles",
            icons = listOf(
                IconItem("Play", Icons.Default.PlayArrow, "Contrôles"),
                IconItem("Pause", Icons.Default.Pause, "Contrôles"),
                IconItem("Stop", Icons.Default.Stop, "Contrôles"),
                IconItem("Next", Icons.Default.SkipNext, "Contrôles"),
                IconItem("Previous", Icons.Default.SkipPrevious, "Contrôles"),
                IconItem("Volume Up", Icons.Default.VolumeUp, "Contrôles"),
                IconItem("Volume Down", Icons.Default.VolumeDown, "Contrôles"),
                IconItem("Mute", Icons.Default.VolumeOff, "Contrôles"),
                IconItem("Settings", Icons.Default.Settings, "Contrôles"),
                IconItem("Power", Icons.Default.PowerSettingsNew, "Contrôles"),
            )
        ),
        IconCategory(
            name = "Navigation",
            icons = listOf(
                IconItem("Home", Icons.Default.Home, "Navigation"),
                IconItem("Back", Icons.Default.ArrowBack, "Navigation"),
                IconItem("Forward", Icons.Default.ArrowForward, "Navigation"),
                IconItem("Up", Icons.Default.ArrowUpward, "Navigation"),
                IconItem("Down", Icons.Default.ArrowDownward, "Navigation"),
                IconItem("Menu", Icons.Default.Menu, "Navigation"),
                IconItem("Close", Icons.Default.Close, "Navigation"),
                IconItem("Check", Icons.Default.Check, "Navigation"),
                IconItem("Cancel", Icons.Default.Cancel, "Navigation"),
            )
        ),
        IconCategory(
            name = "Média",
            icons = listOf(
                IconItem("Music", Icons.Default.MusicNote, "Média"),
                IconItem("Video", Icons.Default.VideoLibrary, "Média"),
                IconItem("Image", Icons.Default.Image, "Média"),
                IconItem("Camera", Icons.Default.CameraAlt, "Média"),
                IconItem("Mic", Icons.Default.Mic, "Média"),
                IconItem("Headset", Icons.Default.Headset, "Média"),
                IconItem("Speaker", Icons.Default.Speaker, "Média"),
                IconItem("Radio", Icons.Default.Radio, "Média"),
            )
        ),
        IconCategory(
            name = "Communication",
            icons = listOf(
                IconItem("Call", Icons.Default.Call, "Communication"),
                IconItem("Message", Icons.Default.Message, "Communication"),
                IconItem("Email", Icons.Default.Email, "Communication"),
                IconItem("Chat", Icons.Default.Chat, "Communication"),
                IconItem("Notifications", Icons.Default.Notifications, "Communication"),
                IconItem("Share", Icons.Default.Share, "Communication"),
            )
        ),
        IconCategory(
            name = "Système",
            icons = listOf(
                IconItem("Lock", Icons.Default.Lock, "Système"),
                IconItem("Unlock", Icons.Default.LockOpen, "Système"),
                IconItem("Sleep", Icons.Default.Bedtime, "Système"),
                IconItem("Restart", Icons.Default.Refresh, "Système"),
                IconItem("Shutdown", Icons.Default.PowerSettingsNew, "Système"),
                IconItem("Brightness", Icons.Default.Brightness6, "Système"),
                IconItem("Battery", Icons.Default.BatteryFull, "Système"),
                IconItem("WiFi", Icons.Default.Wifi, "Système"),
            )
        ),
        IconCategory(
            name = "Actions",
            icons = listOf(
                IconItem("Add", Icons.Default.Add, "Actions"),
                IconItem("Delete", Icons.Default.Delete, "Actions"),
                IconItem("Edit", Icons.Default.Edit, "Actions"),
                IconItem("Save", Icons.Default.Save, "Actions"),
                IconItem("Download", Icons.Default.Download, "Actions"),
                IconItem("Upload", Icons.Default.Upload, "Actions"),
                IconItem("Copy", Icons.Default.ContentCopy, "Actions"),
                IconItem("Cut", Icons.Default.ContentCut, "Actions"),
                IconItem("Paste", Icons.Default.ContentPaste, "Actions"),
            )
        ),
        IconCategory(
            name = "Interface",
            icons = listOf(
                IconItem("Grid", Icons.Default.GridOn, "Interface"),
                IconItem("List", Icons.Default.List, "Interface"),
                IconItem("View", Icons.Default.ViewList, "Interface"),
                IconItem("Visibility", Icons.Default.Visibility, "Interface"),
                IconItem("Visibility Off", Icons.Default.VisibilityOff, "Interface"),
                IconItem("Fullscreen", Icons.Default.Fullscreen, "Interface"),
                IconItem("Expand", Icons.Default.ExpandMore, "Interface"),
                IconItem("Collapse", Icons.Default.ExpandLess, "Interface"),
            )
        ),
        IconCategory(
            name = "Divers",
            icons = listOf(
                IconItem("Star", Icons.Default.Star, "Divers"),
                IconItem("Favorite", Icons.Default.Favorite, "Divers"),
                IconItem("Info", Icons.Default.Info, "Divers"),
                IconItem("Help", Icons.Default.Help, "Divers"),
                IconItem("Warning", Icons.Default.Warning, "Divers"),
                IconItem("Error", Icons.Default.Error, "Divers"),
                IconItem("Check Circle", Icons.Default.CheckCircle, "Divers"),
                IconItem("Schedule", Icons.Default.Schedule, "Divers"),
            )
        )
    )

    val allIcons: List<IconItem> = categories.flatMap { it.icons }

    fun searchIcons(query: String): List<IconItem> {
        val lowerQuery = query.lowercase()
        return allIcons.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.category.lowercase().contains(lowerQuery)
        }
    }

    fun getIconsByCategory(categoryName: String): List<IconItem> {
        return categories.find { it.name == categoryName }?.icons ?: emptyList()
    }
}

/**
 * Dialog de sélection d'icône
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    currentIcon: String?,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val displayedIcons = remember(selectedCategory, searchQuery) {
        when {
            searchQuery.isNotBlank() -> IconLibrary.searchIcons(searchQuery)
            selectedCategory != null -> IconLibrary.getIconsByCategory(selectedCategory!!)
            else -> IconLibrary.allIcons
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.9f),
        title = {
            Text(stringResource(R.string.icon_picker_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Barre de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.icon_picker_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filtres par catégorie
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(stringResource(R.string.icon_picker_all)) }
                    )
                    IconLibrary.categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category.name,
                            onClick = { selectedCategory = category.name },
                            label = { Text(category.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grille d'icônes
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayedIcons, key = { it.name }) { iconItem ->
                        IconPickerItem(
                            iconItem = iconItem,
                            selected = currentIcon == iconItem.name,
                            onClick = { onIconSelected(iconItem.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.icon_picker_done))
            }
        }
    )
}

@Composable
private fun IconPickerItem(
    iconItem: IconItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconItem.icon,
                contentDescription = iconItem.name,
                modifier = Modifier.size(32.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = iconItem.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

