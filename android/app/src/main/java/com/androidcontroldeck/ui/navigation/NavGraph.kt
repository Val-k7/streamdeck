package com.androidcontroldeck.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.localization.applyAppLocale
import com.androidcontroldeck.ui.screens.EditorScreen
import com.androidcontroldeck.ui.screens.ProfileScreen
import com.androidcontroldeck.ui.screens.ProfileListScreen
import com.androidcontroldeck.ui.screens.SettingsScreen
import com.androidcontroldeck.ui.screens.SupportScreen
import com.androidcontroldeck.ui.components.WebViewScreen
import com.androidcontroldeck.network.ConnectionState
import com.androidcontroldeck.data.repository.ProfileSyncState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import com.androidcontroldeck.logging.AppLogger

sealed class Destination(val route: String) {
    data object WebView : Destination("webview")
    data object Profile : Destination("profile")
    data object ProfileList : Destination("profile_list")
    data object Editor : Destination("editor")
    data object Settings : Destination("settings")
    data object Support : Destination("support")
}

@Composable
fun ControlDeckNavHost(
    container: AppContainer,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val currentProfile = container.profileRepository.currentProfile.collectAsState()
    val settingsState = container.preferences.settings.collectAsState(initial = null)
    val isOnline = container.networkMonitor.isOnline.collectAsState(initial = false)
    val isSocketConnected = container.connectionManager.isConnected.collectAsState(initial = false)
    val diagnosticsState = container.diagnosticsRepository.diagnostics.collectAsState()

    val scope = rememberCoroutineScope()

    // Observer les serveurs découverts pour naviguer automatiquement vers l'écran de découverte
    val discoveredServers = container.serverDiscoveryManager.discoveredServers.collectAsState()
    val pairedServers = container.pairedServersRepository.pairedServers.collectAsState(initial = emptyList())
    val connectionState = container.connectionManager.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Éviter les navigations répétées - utiliser une clé basée sur les serveurs découverts
    var lastNavigatedServersKey = remember { mutableStateOf<String?>(null) }

    // Créer une clé stable basée sur le contenu de la liste pour déclencher correctement le LaunchedEffect
    // Utiliser la taille et une chaîne basée sur les IDs triés pour créer une clé unique
    val discoveredServersList = discoveredServers.value
    val discoveredServersKey = discoveredServersList.size.toString() + "_" +
        discoveredServersList.map { it.serverId }.sorted().joinToString(",")

    LaunchedEffect(discoveredServersKey, connectionState.value, currentRoute) {
        val currentState = connectionState.value
        AppLogger.d("NavGraph", "LaunchedEffect triggered: discoveredServers=${discoveredServersList.size}, currentRoute=$currentRoute, connectionState=$currentState, lastNavigatedKey=${lastNavigatedServersKey.value}")

        // Ne rien faire si déjà connecté
        if (currentState is ConnectionState.Connected) {
            AppLogger.d("NavGraph", "Already connected, skipping navigation")
            lastNavigatedServersKey.value = null // Reset pour permettre la navigation après déconnexion
            return@LaunchedEffect
        }

        // Ne rien faire si on est déjà sur l'écran de découverte ou d'appairage
        // Note: Les routes ServerDiscovery et Pairing ont été supprimées (gérées dans React)
        if (false) { // Désactivé car géré dans React
            AppLogger.d("NavGraph", "Already on discovery/pairing screen, skipping navigation")
            return@LaunchedEffect
        }

        // Trouver les serveurs non appairés
        val unpairedServers = discoveredServersList.filter { discoveredServer ->
            pairedServers.value.none { it.serverId == discoveredServer.serverId }
        }

        AppLogger.d("NavGraph", "Unpaired servers: ${unpairedServers.size}, currentRoute: $currentRoute, lastNavigatedKey: ${lastNavigatedServersKey.value}")

        // Note: La découverte et l'appairage sont maintenant gérés dans l'UI React (DiscoveryTab.tsx)
        // Pas besoin de navigation automatique vers des écrans Compose
    }

    LaunchedEffect(settingsState.value?.language) {
        settingsState.value?.language?.let { applyAppLocale(it) }
    }

    val allProfiles = container.profileRepository.profiles.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Destination.WebView.route,
        modifier = modifier
    ) {
        composable(Destination.WebView.route) {
            com.androidcontroldeck.ui.components.WebViewScreen(
                container = container,
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(Destination.Profile.route) {
            // Si aucun profil n'existe ou plusieurs profils sans sélection, rediriger vers la liste
            LaunchedEffect(allProfiles.value.size, currentProfile.value) {
                if (allProfiles.value.isEmpty() ||
                    (allProfiles.value.size > 1 && currentProfile.value == null)) {
                    navController.navigate(Destination.ProfileList.route) {
                        popUpTo(Destination.Profile.route) { inclusive = true }
                    }
                }
            }
            val actionFeedback = container.controlEventSender.actionFeedback.collectAsState()
            val profileSyncState = container.profileRepository.syncState.collectAsState()

            // Afficher les erreurs de synchronisation via Snackbar
            LaunchedEffect(profileSyncState.value) {
                val syncState = profileSyncState.value
                if (syncState != null) {
                    when (syncState.status) {
                        ProfileSyncState.SyncStatus.SUCCESS -> {
                            snackbarHostState.showSnackbar(
                                message = "Profil synchronisé avec succès",
                                duration = androidx.compose.material3.SnackbarDuration.Short
                            )
                        }
                        ProfileSyncState.SyncStatus.ERROR -> {
                            val errorMsg = syncState.errorMessage ?: "Erreur de synchronisation"
                            snackbarHostState.showSnackbar(
                                message = "Erreur de synchronisation: $errorMsg",
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                        }
                        else -> {
                            // SYNCING, RETRYING, IDLE - pas besoin d'afficher
                        }
                    }
                }
            }

            ProfileScreen(
                profile = currentProfile.value,
                onControlEvent = { control, value ->
                    // Envoyer l'événement avec l'action du contrôle si disponible
                    val meta = control.action?.let { action ->
                        mapOf(
                            "actionType" to action.type.name,
                            "actionPayload" to (action.payload ?: "")
                        )
                    } ?: emptyMap()
                    container.controlEventSender.send(control.id, control.type.name, value, meta)
                },
                actionFeedback = actionFeedback.value,
                onNavigateEditor = { navController.navigate(Destination.Editor.route) },
                onNavigateSettings = { navController.navigate(Destination.Settings.route) },
                onNavigateSupport = { navController.navigate(Destination.Support.route) },
                onNavigateProfileList = { navController.navigate(Destination.ProfileList.route) },
                assetCache = container.assetCache,
                showOnboarding = settingsState.value?.onboardingCompleted == false,
                onCompleteOnboarding = {
                    scope.launch { container.preferences.update { it.copy(onboardingCompleted = true) } }
                },
                isOnline = isOnline.value,
                isSocketConnected = isSocketConnected.value
            )
        }

        composable(Destination.ProfileList.route) {
            val allProfiles = container.profileRepository.profiles.collectAsState()
            val currentProfileState = container.profileRepository.currentProfile.collectAsState()

            // Rafraîchir les profils quand on ouvre cette écran
            LaunchedEffect(Unit) {
                container.profileRepository.refresh()
            }

            ProfileListScreen(
                profiles = allProfiles.value,
                currentProfileId = currentProfileState.value?.id,
                onProfileSelected = { profile ->
                    container.profileRepository.selectProfile(profile.id)
                    navController.popBackStack()
                },
                onProfileCreated = { profile ->
                    scope.launch {
                        container.profileRepository.upsertProfile(profile)
                        navController.popBackStack()
                    }
                },
                onProfileDeleted = { profile ->
                    container.profileRepository.deleteProfile(profile.id)
                },
                onProfileDuplicated = { profile ->
                    container.profileRepository.duplicateProfile(profile)
                },
                onProfileRenamed = { profile, newName ->
                    scope.launch {
                        val renamed = profile.copy(name = newName)
                        container.profileRepository.upsertProfile(renamed)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destination.Editor.route) {
            val context = LocalContext.current
            EditorScreen(
                profile = currentProfile.value,
                onProfileChanged = { container.profileRepository.upsertProfile(it) },
                onNavigateBack = { navController.popBackStack() },
                assetCache = container.assetCache,
                onExportProfile = { profile ->
                    scope.launch {
                        val json = container.profileRepository.exportProfile(profile.id) ?: return@launch
                        val file = java.io.File(context.filesDir, "profiles/${profile.id}.json").apply {
                            parentFile?.mkdirs()
                            writeText(json)
                        }
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${com.androidcontroldeck.BuildConfig.APPLICATION_ID}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "${profile.name}.json")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Partager le profil"))
                    }
                },
                onImportProfile = { json ->
                    scope.launch {
                        try {
                            container.profileRepository.importProfile(json)
                            // Optionally show success message
                        } catch (e: Exception) {
                            // Error handling - could show snackbar
                            e.printStackTrace()
                        }
                    }
                }
            )
        }

        composable(Destination.Settings.route) {
            // Utiliser une valeur par défaut si settingsState.value est null
            val currentSettings = settingsState.value ?: com.androidcontroldeck.data.preferences.SettingsState()
            SettingsScreen(
                settings = currentSettings,
                snackbarHostState = snackbarHostState,
                onSettingsChanged = { updatedSettings ->
                    scope.launch {
                        container.preferences.update { updatedSettings }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destination.Support.route) {
            SupportScreen(
                isOnline = isOnline.value,
                isSocketConnected = isSocketConnected.value,
                onNavigateBack = { navController.popBackStack() },
                onSubmitFeedback = { rating, comment, includeLogs ->
                    scope.launch {
                        val diagnostics = diagnosticsState.value
                        val diagnosticsJson = buildString {
                            appendLine("Version: ${diagnostics.versionName}")
                            diagnostics.averageLatencyMs?.let {
                                appendLine("Latence moyenne: ${it.toInt()} ms")
                            }
                            diagnostics.lastNetworkFailure?.let {
                                appendLine("Dernier échec: ${it.message} (${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.at))})")
                            }
                        }
                        container.feedbackRepository.submit(
                            com.androidcontroldeck.data.feedback.FeedbackReport(
                                rating = rating,
                                comment = comment,
                                includeLogs = includeLogs,
                                diagnostics = diagnosticsJson
                            )
                        )
                    }
                }
            )
        }

        // Note: Les écrans ServerDiscovery et Pairing ont été supprimés
        // La découverte et l'appairage sont maintenant gérés dans l'UI React (DiscoveryTab.tsx)
    }
}
