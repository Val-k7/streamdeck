package com.androidcontroldeck.network

import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.logging.DiagnosticsRepository
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.DiscoveredServer
import com.androidcontroldeck.network.model.PairedServer
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/**
 * Valide qu'une chaîne est une adresse IP réseau locale valide (IPv4) Rejette localhost et
 * 127.0.0.1, n'accepte que les adresses réseau locales
 */
private fun isValidIpAddress(ip: String): Boolean {
    if (ip.isBlank()) return false
    val trimmed = ip.trim().lowercase()

    // Rejeter explicitement localhost et 127.0.0.1
    if (trimmed == "localhost" || trimmed == "127.0.0.1" || trimmed.startsWith("127.")) {
        return false
    }

    // Valider les adresses IPv4
    val ipPattern =
            Pattern.compile(
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

class ConnectionManager(
        private val settingsRepository: SettingsRepository,
        private val webSocketClient: WebSocketClient,
        private val authRepository: AuthRepository,
        private val diagnosticsRepository: DiagnosticsRepository,
        private val logger: UnifiedLogger,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val settings: StateFlow<SettingsState> =
            settingsRepository.settings.stateIn(scope, SharingStarted.Eagerly, SettingsState())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    val state: StateFlow<ConnectionState> = webSocketClient.state

    init {
        scope.launch {
            webSocketClient.state.collect { state ->
                _isConnected.value = state is ConnectionState.Connected
            }
        }

        // Écouter les échecs d'authentification et nettoyer le token
        scope.launch {
            webSocketClient.authFailureEvents.collect { reason ->
                AppLogger.w(
                        "ConnectionManager",
                        "Auth failure received (4001): $reason - clearing token"
                )
                authRepository.clearToken()
            }
        }
    }

    suspend fun connect(handshakeSecret: String? = null) {
        val state = settings.value
        val serverIp = state.serverIp.trim()

        // Valider l'IP avant de tenter la connexion
        if (serverIp.isBlank()) {
        package com.androidcontroldeck.network

        import com.androidcontroldeck.data.preferences.SettingsRepository
        import com.androidcontroldeck.data.preferences.SettingsState
        import kotlinx.coroutines.flow.MutableStateFlow
        import kotlinx.coroutines.flow.StateFlow
        import kotlinx.coroutines.flow.asStateFlow

        class ConnectionManager(
                        private val settingsRepository: SettingsRepository,
                        private val webSocketClient: WebSocketClient,
        ) {
                val settings: StateFlow<SettingsState> = settingsRepository.settings

                private val _isConnected = MutableStateFlow(false)
                val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
                val state: StateFlow<ConnectionState> = webSocketClient.state

                suspend fun connect(handshakeSecret: String? = null) {
                        webSocketClient.connect("ws://localhost")
                        _isConnected.value = true
                }
                        "serverName" to server.serverName
                fun disconnect(reason: String? = null) {
                        webSocketClient.disconnect(reason)
                        _isConnected.value = false
                }
                        pairedAt = System.currentTimeMillis(),
                        lastSeen = System.currentTimeMillis(),
                        isAutoConnect = false
                )

        connectToPairedServer(tempPairedServer, handshakeSecret)
    }

    /**
     * Tenter une reconnexion automatique au dernier serveur connecté
     * @param pairedServersRepository Repository des serveurs appairés
     * @return true si une tentative de connexion a été effectuée, false sinon
     */
    suspend fun attemptAutoReconnect(
            pairedServersRepository: com.androidcontroldeck.data.preferences.PairedServersRepository
    ): Boolean {
        // Vérifier l'état actuel - ne pas tenter de reconnexion si déjà connecté ou en cours de
        // connexion
        val currentState = state.value
        if (currentState is ConnectionState.Connected || currentState is ConnectionState.Connecting
        ) {
            AppLogger.d(
                    "ConnectionManager",
                    "Skipping auto-reconnect: already connected or connecting"
            )
            return false
        }

        val lastServer = pairedServersRepository.getLastConnectedServer()

        if (lastServer != null && lastServer.isAutoConnect) {
            AppLogger.d(
                    "ConnectionManager",
                    "Attempting auto-reconnect to: ${lastServer.serverName} (${lastServer.serverId})"
            )
            logger.logInfo(
                    "Auto-reconnect attempt",
                    mapOf(
                            "serverId" to lastServer.serverId,
                            "serverName" to lastServer.serverName,
                            "host" to "${lastServer.host}:${lastServer.port}"
                    )
            )

            try {
                connectToPairedServer(lastServer)
                return true
            } catch (e: Exception) {
                AppLogger.e("ConnectionManager", "Auto-reconnect failed: ${e.message}", e)
                logger.logNetworkError("Auto-reconnect failed", e)
                return false
            }
        }

        AppLogger.d("ConnectionManager", "No server available for auto-reconnect")
        return false
    }
}
