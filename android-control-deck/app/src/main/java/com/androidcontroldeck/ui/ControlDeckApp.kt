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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.R
import com.androidcontroldeck.ui.navigation.ControlDeckNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlDeckApp(container: AppContainer) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.control_deck_title), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(),
                actions = {
                    IconButton(onClick = { /* navigation handled in screens */ }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.control_deck_action_editor)
                        )
                    }
                    IconButton(onClick = { /* navigation handled in screens */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.control_deck_action_settings)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        ControlDeckNavHost(container = container, modifier = Modifier.fillMaxSize())
    }
}
