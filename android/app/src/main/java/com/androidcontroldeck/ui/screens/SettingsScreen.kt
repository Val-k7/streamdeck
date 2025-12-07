package com.androidcontroldeck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androidcontroldeck.R
import com.androidcontroldeck.BuildConfig
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.localization.getAvailableLanguages
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

/**
 * Écran de paramètres système Android simplifié.
 * Note: La configuration du serveur, la découverte et l'appairage sont gérés dans l'UI React (DiscoveryTab.tsx).
 * Cet écran ne contient que les paramètres système Android (langue, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
    onSettingsChanged: (SettingsState) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres système") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Note: La configuration du serveur, la découverte et l'appairage sont gérés dans l'interface principale (React).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            Text(
                "Préférences",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = isLanguageDropdownExpanded,
                onExpandedChange = { isLanguageDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = settings.language,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_language_label, settings.language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("language_dropdown_box")
                )
                DropdownMenu(
                    expanded = isLanguageDropdownExpanded,
                    onDismissRequest = { isLanguageDropdownExpanded = false }
                ) {
                    getAvailableLanguages().forEach { entry ->
                        val tag = entry.key
                        val name = entry.value
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onSettingsChanged(settings.copy(language = tag))
                                isLanguageDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("language_option_$tag")
                        )
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Paramètres sauvegardés")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_save))
            }

            // Section Débogage (uniquement en mode debug)
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()

                Text(
                    "Débogage",
                    style = MaterialTheme.typography.titleMedium
                )

                val context = LocalContext.current
                Button(
                    onClick = {
                        // Ouvrir webdev dans le navigateur
                        val webdevUrl = "http://localhost:5173"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webdevUrl))
                        try {
                            context.startActivity(intent)
                            scope.launch {
                                snackbarHostState.showSnackbar("Ouverture de webdev...")
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Impossible d'ouvrir webdev: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Ouvrir WebDev (localhost:5173)")
                }

                Button(
                    onClick = {
                        // Essayer aussi avec l'IP de l'émulateur (10.0.2.2 pour Android Emulator)
                        val webdevUrl = "http://10.0.2.2:5173"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webdevUrl))
                        try {
                            context.startActivity(intent)
                            scope.launch {
                                snackbarHostState.showSnackbar("Ouverture de webdev (émulateur)...")
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Impossible d'ouvrir webdev: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text("Ouvrir WebDev (10.0.2.2:5173 - Émulateur)")
                }
            }
        }
    }
}
