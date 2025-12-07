package com.androidcontroldeck.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Text
import com.androidcontroldeck.logging.AppLogger
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.rememberNavController
import com.androidcontroldeck.AppContainer
import com.androidcontroldeck.R
import com.androidcontroldeck.ui.navigation.ControlDeckNavHost
import com.androidcontroldeck.ui.navigation.Destination
import com.androidcontroldeck.ui.components.ConnectionStatusBanner
import com.androidcontroldeck.ui.components.WebViewScreen
import com.androidcontroldeck.network.model.DiscoveredServer
import com.androidcontroldeck.network.model.PairedServer
import com.androidcontroldeck.network.ConnectionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.regex.Pattern

/**
 * Gère la détection automatique et la reconnexion de manière centralisée
 */
private suspend fun manageAutoDiscoveryAndReconnect(
    container: AppContainer,
    currentState: ConnectionState,
    discoveredServers: List<DiscoveredServer>,
    pairedServers: List<PairedServer>,
    isDiscovering: Boolean
) {
    // Ne pas tenter de connexion si déjà connecté ou en cours de connexion
    if (currentState is ConnectionState.Connected || currentState is ConnectionState.Connecting) {
        return
    }

    // Log pour déboguer
    AppLogger.d("ControlDeck", "Checking auto-connect: ${discoveredServers.size} discovered, ${pairedServers.size} paired")
    discoveredServers.forEach { discovered ->
        AppLogger.d("ControlDeck", "  Discovered: ${discovered.serverName} (${discovered.serverId})")
    }
    pairedServers.forEach { paired ->
        AppLogger.d("ControlDeck", "  Paired: ${paired.serverName} (${paired.serverId}) autoConnect=${paired.isAutoConnect}")
    }

    // Trouver le premier serveur appairé avec auto-connect activé parmi les serveurs découverts
    val autoConnectServer = discoveredServers.firstOrNull { discoveredServer ->
        val pairedServer = pairedServers.firstOrNull { it.serverId == discoveredServer.serverId }
        val shouldConnect = pairedServer != null && pairedServer.isAutoConnect
        if (shouldConnect) {
            AppLogger.d("ControlDeck", "Found matching server for auto-connect: ${discoveredServer.serverName} (${discoveredServer.serverId})")
        }
        shouldConnect
    }?.let { discoveredServer ->
        pairedServers.firstOrNull { it.serverId == discoveredServer.serverId }
    }

    if (autoConnectServer != null) {
        // Vérifier qu'on n'est pas déjà en train de se connecter à ce serveur
        val currentStateCheck = container.connectionManager.state.value
        if (currentStateCheck is ConnectionState.Connecting) {
            val connectingUrl = currentStateCheck.url
            val serverUrl = "${if (autoConnectServer.protocol == "wss") "wss" else "ws"}://${autoConnectServer.host}:${autoConnectServer.port}/ws"
            if (connectingUrl == serverUrl) {
                AppLogger.d("ControlDeck", "Already connecting to ${autoConnectServer.serverName}, skipping")
                return
            }
        }

        // Serveur appairé avec auto-connect activé, se connecter automatiquement
        AppLogger.d("ControlDeck", "Auto-connecting to discovered paired server: ${autoConnectServer.serverName} (${autoConnectServer.serverId}) at ${autoConnectServer.host}:${autoConnectServer.port}")
        try {
            container.connectionManager.connectToPairedServer(autoConnectServer)
            // Arrêter la détection une fois connecté
            container.serverDiscoveryManager.stopDiscovery()
        } catch (e: Exception) {
            AppLogger.e("ControlDeck", "Auto-connect failed: ${e.message}", e)
            // En cas d'erreur, continuer la détection
        }
        return
    }

    // Si pas de serveur à connecter automatiquement et pas de détection en cours
    if (!isDiscovering) {
        // Attendre un peu avant de redémarrer la détection (éviter les boucles)
        kotlinx.coroutines.delay(3000)

        // Vérifier à nouveau l'état (peut avoir changé pendant le délai)
        val newState = container.connectionManager.state.value
        if (newState is ConnectionState.Disconnected) {
            AppLogger.d("ControlDeck", "Connection lost, restarting automatic discovery")
            container.serverDiscoveryManager.startDiscovery()
        }
    }
}

/**
 * Valide qu'une chaîne est une adresse IP réseau locale valide (IPv4)
 * Rejette localhost et 127.0.0.1, n'accepte que les adresses réseau locales
 */
private fun isValidIpAddress(ip: String): Boolean {
    if (ip.isBlank()) return false
    val trimmed = ip.trim().lowercase()

    // Rejeter explicitement localhost et 127.0.0.1
    if (trimmed == "localhost" || trimmed == "127.0.0.1" || trimmed.startsWith("127.")) {
        return false
    }

    // Valider les adresses IPv4
    val ipPattern = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )
    if (!ipPattern.matcher(trimmed).matches()) {
        return false
    }

    // N'accepter que les adresses réseau locales :
    // - 192.168.x.x (192.168.0.0/16)
    // - 10.x.x.x (10.0.0.0/8)
    // - 172.16.x.x à 172.31.x.x (172.16.0.0/12)
    val parts = trimmed.split(".")
    if (parts.size != 4) return false

    val first = parts[0].toIntOrNull() ?: return false
    val second = parts[1].toIntOrNull() ?: return false

    return when {
        // 192.168.0.0/16
        first == 192 && second == 168 -> true
        // 10.0.0.0/8
        first == 10 -> true
        // 172.16.0.0/12 (172.16.0.0 à 172.31.255.255)
        first == 172 && second in 16..31 -> true
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlDeckApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val connectionState = container.connectionManager.state.collectAsState()
    val pendingActions = container.controlEventSender.pendingActions.collectAsState()
    val settings = container.connectionManager.settings.collectAsState()
    val currentProfile = container.profileRepository.currentProfile.collectAsState()
    val scope = rememberCoroutineScope()

    // Observer les serveurs découverts et l'état de la détection
    val discoveredServers = container.serverDiscoveryManager.discoveredServers.collectAsState()
    val isDiscovering = container.serverDiscoveryManager.isDiscovering.collectAsState()
    val pairedServers = container.pairedServersRepository.pairedServers.collectAsState(initial = emptyList())

    /**
     * Gère la détection automatique et la reconnexion de manière centralisée
     * Cette fonction évite les conflits entre les multiples LaunchedEffect
     * Se déclenche quand l'état de connexion change OU quand un nouveau serveur est découvert
     */
    LaunchedEffect(connectionState.value, discoveredServers.value, pairedServers.value) {
        val currentState = connectionState.value
        val isCurrentlyDiscovering = isDiscovering.value

        when (currentState) {
            is ConnectionState.Connected -> {
                // Si connecté, arrêter la détection pour économiser les ressources
                if (isCurrentlyDiscovering) {
                    AppLogger.d("ControlDeck", "Stopped discovery: already connected")
                    container.serverDiscoveryManager.stopDiscovery()
                }
            }

            is ConnectionState.Disconnected -> {
                // Si déconnecté, gérer la détection et la reconnexion automatique
                manageAutoDiscoveryAndReconnect(
                    container = container,
                    currentState = currentState,
                    discoveredServers = discoveredServers.value,
                    pairedServers = pairedServers.value,
                    isDiscovering = isCurrentlyDiscovering
                )
            }

            is ConnectionState.Connecting -> {
                // En cours de connexion, ne rien faire (éviter les conflits)
            }
        }
    }

    /**
     * Initialisation au démarrage de l'app
     */
    LaunchedEffect(Unit) {
        // Attendre que les paramètres soient chargés
        kotlinx.coroutines.delay(1500)

        val currentState = connectionState.value
        AppLogger.d("ControlDeck", "App initialization, current state: $currentState")

        // Nettoyer les paramètres invalides (localhost, 127.0.0.1, etc.)
        val currentSettings = settings.value
        if (currentSettings.serverIp.isNotBlank() && !isValidIpAddress(currentSettings.serverIp)) {
            AppLogger.d("ControlDeck", "Cleaning invalid IP from settings: ${currentSettings.serverIp}")
            container.preferences.update { it.copy(serverIp = "") }
        }

        if (currentState is ConnectionState.Disconnected) {
            // Essayer d'abord la reconnexion automatique aux serveurs appairés
            val autoReconnected = container.connectionManager.attemptAutoReconnect(
                container.pairedServersRepository
            )

            if (autoReconnected) {
                AppLogger.d("ControlDeck", "Auto-reconnected to paired server")
            } else {
                // Si pas de reconnexion, démarrer la détection automatique
                AppLogger.d("ControlDeck", "No paired server to reconnect, starting automatic discovery")
                container.serverDiscoveryManager.startDiscovery()
            }
        }
    }

    // Arrêter la détection quand l'application est détruite
    DisposableEffect(Unit) {
        onDispose {
            // Nettoyer la détection à la destruction
            container.serverDiscoveryManager.stopDiscovery()
            AppLogger.d("ControlDeck", "Stopped automatic discovery on app destruction")
        }
    }

    // Utiliser ControlDeckNavHost avec WebView comme écran principal
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        ControlDeckNavHost(
            container = container,
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
