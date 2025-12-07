package com.androidcontroldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Sélecteur de couleurs avancé avec palette, hex, RGB et gradients
 */
@Composable
fun ColorPicker(
    currentColor: String?,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = false
) {
    var selectedTab by remember { mutableStateOf<ColorPickerTab>(ColorPickerTab.PALETTE) }
    var hexInput by remember { mutableStateOf(currentColor?.removePrefix("#") ?: "000000") }
    var rgbInput by remember { mutableStateOf(parseHexToRgb(currentColor ?: "#000000")) }
    var alphaInput by remember { mutableStateOf(if (showAlpha) 255 else null) }

    // Synchroniser les inputs avec la couleur actuelle
    LaunchedEffect(currentColor) {
        currentColor?.let { color ->
            hexInput = color.removePrefix("#")
            rgbInput = parseHexToRgb(color)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aperçu de la couleur sélectionnée
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Couleur sélectionnée",
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parseHexColor(currentColor ?: "#000000"))
                )
            }

            // Onglets
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ColorPickerTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.displayName) }
                    )
                }
            }

            // Contenu de l'onglet sélectionné
            when (selectedTab) {
                ColorPickerTab.PALETTE -> {
                    ColorPalettePicker(
                        currentColor = currentColor,
                        onColorSelected = { color ->
                            hexInput = color.removePrefix("#")
                            rgbInput = parseHexToRgb(color)
                            onColorSelected(color)
                        }
                    )
                }
                ColorPickerTab.HEX -> {
                    HexColorPicker(
                        hexInput = hexInput,
                        onHexChanged = { hex ->
                            hexInput = hex
                            try {
                                val color = "#$hex"
                                rgbInput = parseHexToRgb(color)
                                onColorSelected(color)
                            } catch (e: Exception) {
                                // Ignorer les valeurs invalides pendant la saisie
                            }
                        }
                    )
                }
                ColorPickerTab.RGB -> {
                    RgbColorPicker(
                        rgb = rgbInput,
                        alpha = alphaInput,
                        showAlpha = showAlpha,
                        onRgbChanged = { r, g, b, a ->
                            rgbInput = Triple(r, g, b)
                            alphaInput = a
                            val color = rgbToHex(r, g, b, a)
                            hexInput = color.removePrefix("#")
                            onColorSelected(color)
                        }
                    )
                }
            }
        }
    }
}

enum class ColorPickerTab(val displayName: String) {
    PALETTE("Palette"),
    HEX("Hex"),
    RGB("RGB")
}

@Composable
private fun ColorPalettePicker(
    currentColor: String?,
    onColorSelected: (String) -> Unit
) {
    val predefinedColors = remember {
        listOf(
            // Couleurs primaires
            "#FF5722", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            // Nuances de gris
            "#000000", "#212121", "#424242", "#616161", "#757575",
            "#9E9E9E", "#BDBDBD", "#E0E0E0", "#F5F5F5", "#FFFFFF",
            // Couleurs vives
            "#F44336", "#FF1744", "#D50000", "#E91E63", "#C2185B",
            "#7B1FA2", "#512DA8", "#303F9F", "#1976D2", "#0288D1",
            "#00796B", "#388E3C", "#689F38", "#AFB42B", "#FBC02D",
            // Couleurs personnalisées pour streaming
            "#5865F2", // Discord
            "#1DB954", // Spotify
            "#FF0000", // Rouge vif
            "#FF6B00", // Orange
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(predefinedColors.size) { index ->
            val color = predefinedColors[index]
            val isSelected = currentColor?.uppercase() == color.uppercase()

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(parseHexColor(color))
                    .clickable { onColorSelected(color) }
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HexColorPicker(
    hexInput: String,
    onHexChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = hexInput,
            onValueChange = { newValue ->
                if (newValue.length <= 6 && newValue.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                    onHexChanged(newValue.uppercase())
                }
            },
            label = { Text("Code hexadécimal") },
            leadingIcon = { Text("#", modifier = Modifier.padding(start = 16.dp)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true
        )
        Text(
            text = "Entrez un code hexadécimal (6 caractères)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RgbColorPicker(
    rgb: Triple<Int, Int, Int>,
    alpha: Int?,
    showAlpha: Boolean,
    onRgbChanged: (Int, Int, Int, Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ColorSlider(
            label = "Rouge",
            value = rgb.first,
            onValueChange = { onRgbChanged(it, rgb.second, rgb.third, alpha) },
            color = Color.Red
        )
        ColorSlider(
            label = "Vert",
            value = rgb.second,
            onValueChange = { onRgbChanged(rgb.first, it, rgb.third, alpha) },
            color = Color.Green
        )
        ColorSlider(
            label = "Bleu",
            value = rgb.third,
            onValueChange = { onRgbChanged(rgb.first, rgb.second, it, alpha) },
            color = Color.Blue
        )

        if (showAlpha && alpha != null) {
            ColorSlider(
                label = "Alpha",
                value = alpha,
                onValueChange = { onRgbChanged(rgb.first, rgb.second, rgb.third, it) },
                color = Color.Gray
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = rgb.first.toString(),
                onValueChange = { it.toIntOrNull()?.let { value ->
                    onRgbChanged(value.coerceIn(0, 255), rgb.second, rgb.third, alpha)
                } },
                label = { Text("R") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = rgb.second.toString(),
                onValueChange = { it.toIntOrNull()?.let { value ->
                    onRgbChanged(rgb.first, value.coerceIn(0, 255), rgb.third, alpha)
                } },
                label = { Text("G") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = rgb.third.toString(),
                onValueChange = { it.toIntOrNull()?.let { value ->
                    onRgbChanged(rgb.first, rgb.second, value.coerceIn(0, 255), alpha)
                } },
                label = { Text("B") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (showAlpha && alpha != null) {
                OutlinedTextField(
                    value = alpha.toString(),
                    onValueChange = { it.toIntOrNull()?.let { value ->
                        onRgbChanged(rgb.first, rgb.second, rgb.third, value.coerceIn(0, 255))
                    } },
                    label = { Text("A") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Utilitaires
private fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val rgb = parseHexToRgb(hex)
    return Color(rgb.first, rgb.second, rgb.third)
}

private fun parseHexToRgb(hex: String): Triple<Int, Int, Int> {
    val cleanHex = hex.removePrefix("#")
    val r = cleanHex.substring(0, 2).toInt(16)
    val g = cleanHex.substring(2, 4).toInt(16)
    val b = cleanHex.substring(4, 6).toInt(16)
    return Triple(r, g, b)
}

private fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String {
    return if (alpha != null && alpha < 255) {
        "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}${alpha.toString(16).padStart(2, '0')}"
    } else {
        "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
    }
}

