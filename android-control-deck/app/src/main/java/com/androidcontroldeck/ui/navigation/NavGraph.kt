package com.androidcontroldeck.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.localization.applyAppLocale
import com.androidcontroldeck.ui.screens.EditorScreen
import com.androidcontroldeck.ui.screens.ProfileScreen
import com.androidcontroldeck.ui.screens.SettingsScreen
import com.androidcontroldeck.ui.screens.SupportScreen
import kotlinx.coroutines.launch

sealed class Destination(val route: String) {
    data object Profile : Destination("profile")
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
    val profiles = container.profileRepository.profiles.collectAsState()
    val currentProfile = container.profileRepository.currentProfile.collectAsState()
    val settingsState = container.preferences.settings.collectAsState(initial = null)
    val isOnline = container.networkMonitor.isOnline.collectAsState(initial = false)
    val isSocketConnected = container.connectionManager.isConnected.collectAsState(initial = false)
    val storedSecret = container.securePreferences.getHandshakeSecret()
    val diagnosticsState = container.diagnosticsRepository.diagnostics.collectAsState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(settingsState.value?.language) {
        settingsState.value?.language?.let { applyAppLocale(it) }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Profile.route,
        modifier = modifier
    ) {
        composable(Destination.Profile.route) {
            ProfileScreen(
                profile = currentProfile.value,
                onControlEvent = { control, value ->
                    container.controlEventSender.send(control.id, control.type.name, value)
                },
                onNavigateEditor = { navController.navigate(Destination.Editor.route) },
                onNavigateSettings = { navController.navigate(Destination.Settings.route) },
                onNavigateSupport = { navController.navigate(Destination.Support.route) },
                assetCache = container.assetCache,
                showOnboarding = settingsState.value?.onboardingCompleted == false,
                onCompleteOnboarding = {
                    scope.launch { container.preferences.update { copy(onboardingCompleted = true) } }
                },
                isOnline = isOnline.value,
                isSocketConnected = isSocketConnected.value
            )
        }

        composable(Destination.Editor.route) {
            EditorScreen(
                profile = currentProfile.value,
                onProfileChanged = { container.profileRepository.upsertProfile(it) },
                onNavigateBack = { navController.popBackStack() },
                assetCache = container.assetCache
            )
        }

        composable(Destination.Settings.route) {
            val context = LocalContext.current
            SettingsScreen(
                state = settingsState.value,
                handshakeSecret = storedSecret,
                diagnostics = diagnosticsState.value,
                onSettingsChanged = { updates, secret ->
                    scope.launch {
                        container.preferences.update(updates)
                        if (secret.isNotBlank()) {
                            container.securePreferences.saveHandshakeSecret(secret)
                            container.securePreferences.clearAuthToken()
                        }
                        if (secret.isBlank()) container.securePreferences.clearHandshakeSecret()
                        container.connectionManager.connect(secret.ifBlank { null })
                    }
                },
                onSendLogs = {
                    val (_, uri) = container.diagnosticsRepository.exportLogArchive()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Envoyer les logs"))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
