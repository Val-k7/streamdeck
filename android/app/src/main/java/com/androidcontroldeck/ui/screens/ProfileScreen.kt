package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.androidcontroldeck.ui.screens.rememberCachedIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.R
import com.androidcontroldeck.network.model.ActionFeedback
import com.androidcontroldeck.ui.components.ButtonControl
import com.androidcontroldeck.ui.components.FaderControl
import com.androidcontroldeck.ui.components.KnobControl
import com.androidcontroldeck.ui.components.PadControl
import com.androidcontroldeck.ui.components.ToggleControl

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    profile: Profile?,
    onControlEvent: (Control, Float) -> Unit,
    actionFeedback: Map<String, ActionFeedback> = emptyMap(),
    onNavigateEditor: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateSupport: () -> Unit,
    onNavigateProfileList: () -> Unit,
    assetCache: AssetCache,
    showOnboarding: Boolean,
    onCompleteOnboarding: () -> Unit,
    isOnline: Boolean,
    isSocketConnected: Boolean,
    modifier: Modifier = Modifier
) {
    // Variable non utilisée pour l'instant, conservée pour usage futur
    // var showProfileMenu by remember { mutableStateOf(false) }
    if (profile == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onNavigateProfileList) {
                    Text("Créer ou sélectionner un profil")
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barre d'actions rapides (settings) intégrée proprement dans le pad
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateProfileList,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = "Profils",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateEditor,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Éditer",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Pad principal avec les contrôles
            LazyVerticalGrid(
                columns = GridCells.Fixed(profile.cols),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            items(profile.controls, key = { it.id }, span = { GridItemSpan(it.colSpan) }) { control ->
                val heightRatio = (control.rowSpan.toFloat() / control.colSpan.toFloat()).coerceAtLeast(0.75f)
                val icon by rememberCachedIcon(control.icon, assetCache)
                val feedback = actionFeedback[control.id]
                when (control.type) {
                    ControlType.BUTTON -> ButtonControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        onClick = { onControlEvent(control, 1f) },
                        onLongClick = { onControlEvent(control, -1f) },
                        feedback = feedback,
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxSize()
                            .aspectRatio(heightRatio)
                            .testTag("control_${control.id}")
                    )

                    ControlType.TOGGLE -> ToggleControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        icon = icon,
                        onToggle = { state -> onControlEvent(control, if (state) 1f else 0f) },
                        feedback = feedback,
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxSize()
                            .aspectRatio(heightRatio)
                            .testTag("control_${control.id}")
                    )

                    ControlType.FADER -> FaderControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        minValue = control.minValue,
                        maxValue = control.maxValue,
                        onValueChanged = { value -> onControlEvent(control, value) },
                        feedback = feedback,
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxSize()
                            .aspectRatio(heightRatio)
                            .testTag("control_${control.id}")
                    )

                    ControlType.KNOB -> KnobControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        minValue = control.minValue,
                        maxValue = control.maxValue,
                        onDelta = { value -> onControlEvent(control, value) },
                        feedback = feedback,
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxSize()
                            .aspectRatio(1f)
                            .testTag("control_${control.id}")
                    )

                    ControlType.PAD -> PadControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        onClick = { onControlEvent(control, 1f) },
                        onLongClick = { onControlEvent(control, -1f) },
                        feedback = feedback,
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxSize()
                            .aspectRatio(heightRatio)
                            .testTag("control_${control.id}")
                    )
                }
            }
        }

            if (showOnboarding) {
                OnboardingOverlay(
                    isOnline = isOnline,
                    isSocketConnected = isSocketConnected,
                    onNavigateSettings = onNavigateSettings,
                    onNavigateSupport = onNavigateSupport,
                    onComplete = onCompleteOnboarding
                )
            }
        }
    }
}

// rememberCachedIcon est défini dans EditorScreen.kt pour éviter les conflits

@Composable
private fun OnboardingOverlay(
    isOnline: Boolean,
    isSocketConnected: Boolean,
    onNavigateSettings: () -> Unit,
    onNavigateSupport: () -> Unit,
    onComplete: () -> Unit,
) {
    val networkChecked = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bienvenue !",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Suivez ces 3 étapes pour votre première connexion :",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text("1. Vérifiez que le téléphone et le serveur sont sur le même réseau local.")
                Text("2. Autorisez l'accès réseau local (pare-feu et Wi-Fi).")
                Text("3. Lancez une commande test dans l'éditeur pour valider les contrôles.")

                if (networkChecked.value) {
                    val onlineLabel = if (isOnline) "Réseau disponible" else "Aucune connectivité détectée"
                    val socketLabel = if (isSocketConnected) "Canal temps réel actif" else "WebSocket non connecté"
                    Text("État réseau : $onlineLabel")
                    Text("État de session : $socketLabel")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { networkChecked.value = true }) {
                        Text("Vérifier le réseau")
                    }
                    TextButton(onClick = onNavigateSettings) {
                        Text("Ouvrir les paramètres")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onNavigateSupport) {
                        Text("Lire le tutoriel complet")
                    }
                    Button(onClick = onComplete) {
                        Text("Je suis prêt")
                    }
                }
            }
        }
    }
}
