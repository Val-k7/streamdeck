package com.androidcontroldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.ui.screens.rememberCachedIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue

/**
 * Mode aperçu en temps réel dans l'éditeur
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPreview(
    profile: Profile,
    assetCache: AssetCache,
    onControlClick: (Control) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPreviewMode by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aperçu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Switch(
                    checked = isPreviewMode,
                    onCheckedChange = { isPreviewMode = it },
                    thumbContent = if (isPreviewMode) {
                        {
                            Icon(
                                imageVector = Icons.Default.Preview,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }

            if (isPreviewMode) {
                Divider()

                PreviewGrid(
                    profile = profile,
                    assetCache = assetCache,
                    onControlClick = onControlClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Activez l'aperçu pour voir un rendu en temps réel de votre profil",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewGrid(
    profile: Profile,
    assetCache: AssetCache,
    onControlClick: (Control) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = 60.dp // Taille fixe pour l'aperçu

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Créer une grille pour l'aperçu
        repeat(profile.rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(profile.cols) { col ->
                    // Trouver le contrôle à cette position
                    val control = profile.controls.find { ctrl ->
                        ctrl.row <= row && row < ctrl.row + ctrl.rowSpan &&
                        ctrl.col <= col && col < ctrl.col + ctrl.colSpan
                    }

                    val cellWidth = control?.let {
                        if (col == it.col) cellSize * it.colSpan else null
                    } ?: cellSize

                    if (control != null && col == control.col) {
                        // Afficher le contrôle
                        PreviewControlCell(
                            control = control,
                            cellSize = cellSize,
                            assetCache = assetCache,
                            onClick = { onControlClick(control) },
                            modifier = Modifier.width(cellWidth)
                        )
                    } else if (control == null) {
                        // Cellule vide
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewControlCell(
    control: Control,
    cellSize: androidx.compose.ui.unit.Dp,
    assetCache: AssetCache,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconState: androidx.compose.runtime.State<androidx.compose.ui.graphics.ImageBitmap?> = rememberCachedIcon(control.icon, assetCache)
    val color = control.colorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .height(cellSize * control.rowSpan)
            .clip(RoundedCornerShape(8.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                iconState.value?.let { icon ->
                    androidx.compose.foundation.Image(
                        bitmap = icon,
                        contentDescription = control.label,
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                } ?: run {
                    // Icône par défaut selon le type
                    val defaultIcon = when (control.type) {
                        ControlType.BUTTON -> Icons.Default.PlayArrow
                        ControlType.TOGGLE -> Icons.Default.ToggleOn
                        ControlType.FADER -> Icons.Default.Tune
                        ControlType.KNOB -> Icons.Default.RotateRight
                        ControlType.PAD -> Icons.Default.GridView
                    }
                    Icon(
                        imageVector = defaultIcon,
                        contentDescription = control.label,
                        modifier = Modifier.size(20.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                }

                if (control.label.isNotEmpty()) {
                    Text(
                        text = control.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

