package com.androidcontroldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R

/**
 * Barre d'outils avancée pour l'éditeur de profils
 */
@Composable
fun EditorToolbar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onAlignLeft: () -> Unit,
    onAlignCenter: () -> Unit,
    onAlignRight: () -> Unit,
    onDistributeHorizontally: () -> Unit,
    onDistributeVertically: () -> Unit,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Groupe Undo/Redo
            Row {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.editor_undo))
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.editor_redo))
                }
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Groupe Zoom
            Row {
                IconButton(onClick = onZoomOut) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.editor_zoom_out))
                }
                IconButton(onClick = onResetZoom) {
                    Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.editor_reset_zoom))
                }
                IconButton(onClick = onZoomIn) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.editor_zoom_in))
                }
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Groupe Alignement
            Row {
                IconButton(onClick = onAlignLeft) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.editor_align_left))
                }
                IconButton(onClick = onAlignCenter) {
                    Icon(Icons.Default.CropFree, contentDescription = stringResource(R.string.editor_align_center))
                }
                IconButton(onClick = onAlignRight) {
                    Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.editor_align_right))
                }
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Groupe Distribution
            Row {
                IconButton(onClick = onDistributeHorizontally) {
                    Icon(Icons.Default.ViewColumn, contentDescription = stringResource(R.string.editor_distribute_horizontal))
                }
                IconButton(onClick = onDistributeVertically) {
                    Icon(Icons.Default.ViewAgenda, contentDescription = stringResource(R.string.editor_distribute_vertical))
                }
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Groupe Groupes
            Row {
                IconButton(onClick = onGroup) {
                    Icon(Icons.Default.Layers, contentDescription = stringResource(R.string.editor_group))
                }
                IconButton(onClick = onUngroup) {
                    Icon(Icons.Default.LayersClear, contentDescription = stringResource(R.string.editor_ungroup))
                }
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Groupe Verrouillage
            Row {
                IconButton(onClick = onLock) {
                    Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.editor_lock))
                }
                IconButton(onClick = onUnlock) {
                    Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.editor_unlock))
                }
            }
        }
    }
}

