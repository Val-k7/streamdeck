package com.androidcontroldeck.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.localization.applyAppLocale
import com.androidcontroldeck.ui.screens.EditorScreen
import com.androidcontroldeck.ui.screens.ProfileScreen
import com.androidcontroldeck.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

sealed class Destination(val route: String) {
    data object Profile : Destination("profile")
    data object Editor : Destination("editor")
    data object Settings : Destination("settings")
}

@Composable
fun ControlDeckNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val profiles = container.profileRepository.profiles.collectAsState()
    val currentProfile = container.profileRepository.currentProfile.collectAsState()
    val settingsState = container.preferences.settings.collectAsState(initial = null)
    val storedSecret = container.securePreferences.getHandshakeSecret()

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
                assetCache = container.assetCache
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
            SettingsScreen(
                state = settingsState.value,
                handshakeSecret = storedSecret,
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
