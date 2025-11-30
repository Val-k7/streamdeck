package com.androidcontroldeck.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.ui.navigation.ControlDeckNavHost
import com.androidcontroldeck.ui.components.ConnectionStatusBanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlDeckApp(container: AppContainer) {
    val snackbarHostState = remember { SnackbarHostState() }
    val connectionState = container.connectionManager.state.collectAsState()
    val pendingActions = container.controlEventSender.pendingActions.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Android Control Deck") },
                colors = TopAppBarDefaults.topAppBarColors(),
                actions = {
                    IconButton(onClick = { /* navigation handled in screens */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editor")
                    }
                    IconButton(onClick = { /* navigation handled in screens */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ConnectionStatusBanner(
                state = connectionState.value,
                pendingCount = pendingActions.value.size,
                onReconnect = { scope.launch { container.connectionManager.connect() } },
                onReplayQueue = { container.controlEventSender.flushPending() },
                onPurgeQueue = { container.controlEventSender.purgePending() }
            )
            ControlDeckNavHost(container = container, modifier = Modifier.fillMaxSize())
        }
    }
}
