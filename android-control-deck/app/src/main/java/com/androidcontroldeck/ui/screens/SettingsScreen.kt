package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.data.preferences.ThemeMode

@Composable
fun SettingsScreen(
    state: SettingsState?,
    onSettingsChanged: (SettingsState.() -> SettingsState) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == null) {
        Text("Chargement des paramètres...", modifier = modifier.padding(16.dp))
        return
    }

    val ip = remember(state.serverIp) { mutableStateOf(state.serverIp) }
    val port = remember(state.serverPort) { mutableStateOf(state.serverPort.toString()) }
    val heartbeat = remember(state.heartbeatIntervalMs) { mutableStateOf(state.heartbeatIntervalMs.toFloat()) }
    val latency = remember(state.latencyMs) { mutableStateOf(state.latencyMs.toFloat()) }
    val theme = remember(state.themeMode) { mutableStateOf(state.themeMode) }
    val language = remember(state.language) { mutableStateOf(state.language) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(
            title = { Text("Paramètres") },
            colors = TopAppBarDefaults.topAppBarColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = ip.value,
            onValueChange = { ip.value = it },
            label = { Text("Adresse IP du serveur") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port.value,
            onValueChange = { port.value = it.filter { c -> c.isDigit() } },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Intervalle de heartbeat: ${heartbeat.value.toInt()} ms", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = heartbeat.value,
            onValueChange = { heartbeat.value = it },
            valueRange = 1_000f..15_000f,
            steps = 13
        )

        Text("Latence cible: ${latency.value.toInt()} ms", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = latency.value,
            onValueChange = { latency.value = it },
            valueRange = 8f..64f,
            steps = 7
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Thème: ${theme.value}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                AssistChip(onClick = { theme.value = mode }, label = { Text(mode.name) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Langue: ${language.value}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("fr", "en").forEach { lang ->
                AssistChip(onClick = { language.value = lang }, label = { Text(lang.uppercase()) })
            }
        }

        Button(
            onClick = {
                onSettingsChanged {
                    copy(
                        serverIp = ip.value,
                        serverPort = port.value.toIntOrNull() ?: state.serverPort,
                        heartbeatIntervalMs = heartbeat.value.toInt(),
                        latencyMs = latency.value.toInt(),
                        themeMode = theme.value,
                        language = language.value
                    )
                }
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Onboarding rapide : autorisez l'accès réseau local et restez connecté au même Wi-Fi que le serveur pour une latence minimale.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
