package com.example.controldeck.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.controldeck.model.AccessibilityPreferences
import com.example.controldeck.ui.components.accessibleControl

@Composable
fun SettingsScreen(
    preferences: AccessibilityPreferences,
    onPreferencesChanged: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Text(
            text = "Accessibilité",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        SettingToggle(
            title = "Animations réduites",
            description = "Limiter les mouvements et transitions pour réduire l'inconfort.",
            checked = preferences.reduceMotion,
            onCheckedChange = { onPreferencesChanged(preferences.copy(reduceMotion = it)) }
        )
        VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingToggle(
            title = "Retour haptique",
            description = "Activer ou couper les vibrations des contrôles.",
            checked = preferences.hapticsEnabled,
            onCheckedChange = { onPreferencesChanged(preferences.copy(hapticsEnabled = it)) }
        )
        VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingToggle(
            title = "Police agrandie",
            description = "Augmenter la taille des textes pour une meilleure lisibilité.",
            checked = preferences.largeTextEnabled,
            onCheckedChange = { onPreferencesChanged(preferences.copy(largeTextEnabled = it)) }
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = description, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.accessibleControl(
                label = "$title: ${'$'}{if (checked) "activé" else "désactivé"}",
                onClick = { onCheckedChange(!checked) },
                role = Role.Switch
            ),
            thumbContent = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFB4E600),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF6B6B6B)
            )
        )
    }
}
