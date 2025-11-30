package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.data.model.Control
import com.androidcontroldeck.data.model.ControlType
import com.androidcontroldeck.data.model.Profile
import com.androidcontroldeck.data.storage.AssetCache
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
    assetCache: AssetCache,
    modifier: Modifier = Modifier
) {
    if (profile == null) {
        Text(
            text = "Chargement du profil...",
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(profile.cols),
        modifier = modifier.fillMaxSize(),
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
                )
            }
        }
    }
}

@Composable
private fun rememberCachedIcon(name: String?, assetCache: AssetCache): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, name, assetCache) {
        value = assetCache.loadIcon(name)
    }
}
