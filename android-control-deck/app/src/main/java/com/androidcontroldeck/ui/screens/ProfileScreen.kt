package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
import com.androidcontroldeck.R
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
    onNavigateEditor: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateSupport: () -> Unit,
    assetCache: AssetCache,
    showOnboarding: Boolean,
    onCompleteOnboarding: () -> Unit,
    isOnline: Boolean,
    isSocketConnected: Boolean,
    modifier: Modifier = Modifier
) {
    if (profile == null) {
        Text(
            text = stringResource(R.string.profile_loading),
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(profile.cols),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(profile.controls, key = { it.id }, span = { GridItemSpan(it.colSpan) }) { control ->
                val heightRatio = (control.rowSpan.toFloat() / control.colSpan.toFloat()).coerceAtLeast(0.75f)
                val icon by rememberCachedIcon(control.icon, assetCache)
                when (control.type) {
                    ControlType.BUTTON -> ButtonControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        icon = icon,
                        onTap = { onControlEvent(control, 1f) },
                        onLongPress = { onControlEvent(control, -1f) },
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
                        modifier = Modifier
                            .animateItemPlacement()
                            .fillMaxSize()
                            .aspectRatio(1f)
                            .testTag("control_${control.id}")
                    )

                    ControlType.PAD -> PadControl(
                        label = control.label,
                        colorHex = control.colorHex,
                        onPress = { onControlEvent(control, 1f) },
                        onLongPress = { onControlEvent(control, -1f) },
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

@Composable
private fun rememberCachedIcon(name: String?, assetCache: AssetCache): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, name, assetCache) {
        value = assetCache.loadIcon(name)
    }
}

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
