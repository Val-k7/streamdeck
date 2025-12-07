package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    isOnline: Boolean,
    isSocketConnected: Boolean,
    onNavigateBack: () -> Unit,
    onSubmitFeedback: suspend (rating: Int, comment: String, includeLogs: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val rating = remember { mutableStateOf(4f) }
    val comment = remember { mutableStateOf("") }
    val includeLogs = remember { mutableStateOf(true) }
    val statusMessage = remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TopAppBar(
                title = { Text("Centre d'aide") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Retour") }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Raccourcis support", style = MaterialTheme.typography.titleMedium)
                    Text("FAQ rapide :")
                    val faqEntries = listOf(
                        "Connexion impossible" to "Vérifiez le Wi-Fi, l'IP et le port dans Paramètres.",
                        "Actions lentes" to "Réduisez la latence cible et restez sur le même réseau.",
                        "Bouton inactif" to "Testez dans l'éditeur et vérifiez le mapping coté serveur."
                    )
                    faqEntries.forEach { (question, answer) ->
                        Text("• $question : $answer", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Diagnostic réseau et pare-feu", style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.support_connectivity_status, if (isOnline) "OK" else "À corriger", if (isSocketConnected) "connecté" else "non connecté"))
                    Text(stringResource(R.string.support_firewall_status, if (isSocketConnected) "ouvert" else "à ouvrir"))
                    Text("• Désactivez le VPN si le trafic local est bloqué.")
                    Text("• Testez en HTTP avant d'activer TLS/pin pour isoler les erreurs de certificat.")
                    Spacer(Modifier.height(8.dp))
                    RowLink("support@controldeck.app") { uriHandler.openUri("mailto:support@controldeck.app") }
                    RowLink("Documentation") { uriHandler.openUri("https://docs.controldeck.app") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Feedback & rapport", style = MaterialTheme.typography.titleMedium)
                    Text("Notez votre expérience (1-5)")
                    Slider(
                        value = rating.value,
                        onValueChange = { rating.value = it },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                    OutlinedTextField(
                        value = comment.value,
                        onValueChange = { comment.value = it },
                        label = { Text("Commentaire") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    RowSwitch(
                        label = "Inclure un snapshot des logs réseau et paramètres",
                        checked = includeLogs.value,
                        onCheckedChange = { includeLogs.value = it }
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                onSubmitFeedback(rating.value.toInt(), comment.value.trim(), includeLogs.value)
                                statusMessage.value = "Merci pour votre retour !"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Envoyer le rapport")
                    }
                    statusMessage.value?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun RowLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label) }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        RowLine(checked, onCheckedChange)
    }
}

@Composable
private fun RowLine(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(if (checked) "Logs activés" else "Logs désactivés")
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
