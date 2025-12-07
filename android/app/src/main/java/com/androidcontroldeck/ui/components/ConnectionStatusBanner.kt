package com.androidcontroldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.network.ConnectionState

@Composable
fun ConnectionStatusBanner(
    state: ConnectionState,
    pendingCount: Int,
    onReconnect: () -> Unit,
    onReplayQueue: () -> Unit,
    onPurgeQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, description, color) = when (state) {
        is ConnectionState.Connected -> Triple(
            "Connecté",
            if (pendingCount > 0) "${pendingCount} action(s) en attente" else "Synchronisé",
            MaterialTheme.colorScheme.primaryContainer,
        )

        is ConnectionState.Connecting -> Triple(
            "Reconnexion…",
            "Tentative en cours",
            MaterialTheme.colorScheme.tertiaryContainer,
        )

        is ConnectionState.Disconnected -> Triple(
            "Hors ligne",
            state.reason ?: "Déconnecté du serveur",
            MaterialTheme.colorScheme.errorContainer,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }

            if (state !is ConnectionState.Connected) {
                Button(onClick = onReconnect) { Text("Reconnexion") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onReplayQueue, enabled = pendingCount > 0) {
                Text("Rejouer la file (${pendingCount})")
            }
            TextButton(onClick = onPurgeQueue, enabled = pendingCount > 0) {
                Text("Purger")
            }
        }
    }
}
