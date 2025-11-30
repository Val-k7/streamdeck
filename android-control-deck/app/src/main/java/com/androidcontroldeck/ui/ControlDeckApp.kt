package com.androidcontroldeck.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.ui.navigation.ControlDeckNavHost
import com.androidcontroldeck.ui.navigation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlDeckApp(container: AppContainer) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Android Control Deck") },
                colors = TopAppBarDefaults.topAppBarColors(),
                actions = {
                    IconButton(onClick = {
                        navController.navigate(
                            Destination.Editor.route,
                            navOptions { launchSingleTop = true }
                        )
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editor")
                    }
                    IconButton(onClick = {
                        navController.navigate(
                            Destination.Settings.route,
                            navOptions { launchSingleTop = true }
                        )
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        navController.navigate(
                            Destination.Support.route,
                            navOptions { launchSingleTop = true }
                        )
                    }) {
                        Icon(Icons.Default.Help, contentDescription = "Aide et support")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        ControlDeckNavHost(
            container = container,
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
