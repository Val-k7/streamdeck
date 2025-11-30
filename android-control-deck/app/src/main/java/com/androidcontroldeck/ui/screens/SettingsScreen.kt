package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.annotation.StringRes
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.androidcontroldeck.R
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.data.preferences.ThemeMode
import com.androidcontroldeck.logging.DiagnosticsState
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    state: SettingsState?,
    handshakeSecret: String?,
    diagnostics: DiagnosticsState,
    onSettingsChanged: (SettingsState.() -> SettingsState, String) -> Unit,
    onSendLogs: (android.content.Context) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == null) {
        Text(stringResource(R.string.settings_loading), modifier = modifier.padding(16.dp))
        return
    }

    val context = LocalContext.current
    val ip = remember(state.serverIp) { mutableStateOf(state.serverIp) }
    val port = remember(state.serverPort) { mutableStateOf(state.serverPort.toString()) }
    val heartbeat = remember(state.heartbeatIntervalMs) { mutableStateOf(state.heartbeatIntervalMs.toFloat()) }
    val latency = remember(state.latencyMs) { mutableStateOf(state.latencyMs.toFloat()) }
    val theme = remember(state.themeMode) { mutableStateOf(state.themeMode) }
    val language = remember(state.language) {
        mutableStateOf(languageOptions.firstOrNull { it.tag == state.language }?.tag ?: SYSTEM_LANGUAGE_TAG)
    }
    val useTls = remember(state.useTls) { mutableStateOf(state.useTls) }
    val pinnedCert = remember(state.pinnedCertSha256) { mutableStateOf(state.pinnedCertSha256.orEmpty()) }
    val secret = remember(handshakeSecret) { mutableStateOf(handshakeSecret.orEmpty()) }
    val themeLabel = stringResource(theme.value.labelRes())
    val selectedLanguageLabel = languageOptions.firstOrNull { it.tag == language.value }
        ?.let { stringResource(it.labelRes) }
        ?: language.value.uppercase()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.settings_title),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = ip.value,
            onValueChange = { ip.value = it },
            label = { Text(stringResource(R.string.settings_server_ip_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port.value,
            onValueChange = { port.value = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.settings_port_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            stringResource(R.string.settings_heartbeat_label, heartbeat.value.toInt()),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = heartbeat.value,
            onValueChange = { heartbeat.value = it },
            valueRange = 1_000f..15_000f,
            steps = 13
        )

        Text(
            stringResource(R.string.settings_latency_label, latency.value.toInt()),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = latency.value,
            onValueChange = { latency.value = it },
            valueRange = 8f..64f,
            steps = 7
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.settings_theme_label, themeLabel), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                AssistChip(onClick = { theme.value = mode }, label = { Text(stringResource(mode.labelRes())) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_language_label, selectedLanguageLabel),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            languageOptions.forEach { option ->
                AssistChip(
                    onClick = { language.value = option.tag },
                    label = { Text(stringResource(option.labelRes)) },
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(
                            R.string.settings_language_option_content_description,
                            stringResource(option.labelRes)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column { Text(stringResource(R.string.settings_tls_label)) }
            Switch(checked = useTls.value, onCheckedChange = { useTls.value = it })
        }
        OutlinedTextField(
            value = pinnedCert.value,
            onValueChange = { pinnedCert.value = it.trim() },
            label = { Text(stringResource(R.string.settings_pinned_cert_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secret.value,
            onValueChange = { secret.value = it },
            label = { Text(stringResource(R.string.settings_handshake_secret_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSettingsChanged(
                    {
                        copy(
                            serverIp = ip.value,
                            serverPort = port.value.toIntOrNull() ?: state.serverPort,
                            heartbeatIntervalMs = heartbeat.value.toInt(),
                            latencyMs = latency.value.toInt(),
                            themeMode = theme.value,
                            language = language.value,
                            useTls = useTls.value,
                            pinnedCertSha256 = pinnedCert.value.ifBlank { null }
                        )
                    },
                    secret.value
                )
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_save))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        DiagnosticRow("Version", diagnostics.versionName)
        DiagnosticRow(
            label = "Latence moyenne",
            value = diagnostics.averageLatencyMs?.let { "${it.toInt()} ms" } ?: "Non disponible"
        )
        DiagnosticRow(
            label = "Dernier échec réseau",
            value = diagnostics.lastNetworkFailure?.let {
                val formattedDate = DateFormat.getDateTimeInstance().format(Date(it.at))
                "${it.message} ($formattedDate)"
            } ?: "Aucun"
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onSendLogs(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Envoyer les logs")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onOpenSupport, modifier = Modifier.fillMaxWidth()) {
            Text("Centre d'aide / Feedback")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.settings_onboarding_note),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
