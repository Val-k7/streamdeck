package com.androidcontroldeck.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.ui.navigation.ControlDeckNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlDeckApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsState = container.preferences.settings.collectAsState(initial = SettingsState())

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        ControlDeckNavHost(
                container = container,
                navController = navController,
                snackbarHostState = snackbarHostState,
                settings = settingsState.value,
                modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
