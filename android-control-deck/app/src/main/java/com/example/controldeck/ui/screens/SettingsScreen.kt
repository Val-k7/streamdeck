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
import androidx.compose.ui.res.stringResource
import com.androidcontroldeck.R

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
            text = stringResource(R.string.settings_accessibility_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        SettingToggle(
            title = stringResource(R.string.settings_reduce_motion_title),
            description = stringResource(R.string.settings_reduce_motion_description),
            checked = preferences.reduceMotion,
            onCheckedChange = { onPreferencesChanged(preferences.copy(reduceMotion = it)) }
        )
        VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingToggle(
            title = stringResource(R.string.settings_haptics_title),
            description = stringResource(R.string.settings_haptics_description),
            checked = preferences.hapticsEnabled,
            onCheckedChange = { onPreferencesChanged(preferences.copy(hapticsEnabled = it)) }
        )
        VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingToggle(
            title = stringResource(R.string.settings_large_text_title),
            description = stringResource(R.string.settings_large_text_description),
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
                label = stringResource(
                    R.string.settings_toggle_label_format,
                    title,
                    stringResource(if (checked) R.string.settings_toggle_state_on else R.string.settings_toggle_state_off)
                ),
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
