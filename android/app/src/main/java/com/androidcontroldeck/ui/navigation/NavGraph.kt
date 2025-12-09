package com.androidcontroldeck.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.localization.applyAppLocale
import com.androidcontroldeck.ui.components.ServerWebViewScreen
import com.androidcontroldeck.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

sealed class Destination(val route: String) {
    data object WebView : Destination("webview")
    data object Settings : Destination("settings")
}

@Composable
fun ControlDeckNavHost(
    container: AppContainer,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    settings: SettingsState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(settings.language) { applyAppLocale(settings.language) }
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Destination.WebView.route,
        modifier = modifier
    ) {
        composable(Destination.WebView.route) {
            ServerWebViewScreen(
                container = container,
                navController = navController,
                settings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(Destination.Settings.route) {
            SettingsScreen(
                settings = settings,
                snackbarHostState = snackbarHostState,
                onSettingsChanged = { updatedSettings ->
                    scope.launch { container.preferences.update { updatedSettings } }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
