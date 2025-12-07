package com.androidcontroldeck.network

import com.androidcontroldeck.logging.AppLogger
import com.androidcontroldeck.data.preferences.SettingsRepository
import com.androidcontroldeck.data.preferences.SettingsState
import com.androidcontroldeck.logging.DiagnosticsRepository
import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.DiscoveredServer
import com.androidcontroldeck.network.model.PairedServer
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

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

class ConnectionManager(
    private val settingsRepository: SettingsRepository,
    private val webSocketClient: WebSocketClient,
    private val authRepository: AuthRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val logger: UnifiedLogger,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val settings: StateFlow<SettingsState> = settingsRepository.settings
        .stateIn(scope, SharingStarted.Eagerly, SettingsState())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    val state: StateFlow<ConnectionState> = webSocketClient.state

    init {
        scope.launch {
            webSocketClient.state.collect { state ->
                _isConnected.value = state is ConnectionState.Connected
            }
        }
    }

    suspend fun connect(handshakeSecret: String? = null) {
        val state = settings.value
        val serverIp = state.serverIp.trim()

        // Valider l'IP avant de tenter la connexion
        if (serverIp.isBlank()) {
            AppLogger.e("ConnectionManager", "Cannot connect: server IP is empty")
            logger.logNetworkError("Connection failed", IllegalArgumentException("Server IP is not configured"))
            return
        }

        if (!isValidIpAddress(serverIp)) {
            val errorMsg = if (serverIp.lowercase().trim() == "localhost" || serverIp.trim().startsWith("127.")) {
                "L'adresse IP doit être une adresse réseau locale (192.168.x.x, 10.x.x.x, ou 172.16-31.x.x), pas localhost ou 127.0.0.1"
            } else {
                "Format d'adresse IP invalide ou non-réseau local: $serverIp"
            }
            AppLogger.e("ConnectionManager", "Cannot connect: $errorMsg")
            logger.logNetworkError("Connection failed", IllegalArgumentException(errorMsg))
            return
        }

        AppLogger.d("ConnectionManager", "connect() called: IP=$serverIp, Port=${state.serverPort}, TLS=${state.useTls}")
        val scheme = if (state.useTls) "wss" else "ws"
        val httpScheme = if (state.useTls) "https" else "http"
        val certificatePinner = state.pinnedCertSha256?.takeIf { it.isNotBlank() }?.let { fingerprint ->
            CertificatePinner.Builder()
                .add(serverIp, "sha256/$fingerprint")
                .build()
        }

        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .apply { certificatePinner?.let { certificatePinner(it) } }
            .build()

        webSocketClient.updateClient(client)
        webSocketClient.updateConfig(WebSocketConfig(heartbeatIntervalMs = state.heartbeatIntervalMs.toLong()))

        val savedSecret = handshakeSecret?.takeIf { it.isNotBlank() }
            ?: authRepository.getHandshakeSecret()
            ?: "change-me" // Valeur par défaut si aucun secret n'est configuré
        AppLogger.d("ConnectionManager", "Using handshake secret: ${if (savedSecret == "change-me") "change-me (default)" else "***"}")
        val existingToken = authRepository.getToken()
        AppLogger.d("ConnectionManager", "Existing token: ${if (existingToken != null) "found" else "none"}")
        val token = existingToken ?: savedSecret.let { secret ->
            AppLogger.d("ConnectionManager", "Attempting handshake to $httpScheme://$serverIp:${state.serverPort}/handshake")
            runCatching {
                authRepository.performHandshake(
                    "$httpScheme://$serverIp:${state.serverPort}",
                    secret,
                    certificatePinner
                )
            }.onFailure { error ->
                AppLogger.e("ConnectionManager", "Handshake failed: ${error.message}", error)
                logger.logNetworkError("Handshake failed", error)
                diagnosticsRepository.recordNetworkFailure("Handshake failed: ${error.message}", error)
            }.onSuccess { token ->
                AppLogger.d("ConnectionManager", "Handshake successful, token received")
            }.getOrNull()
        }

        val url = "$scheme://$serverIp:${state.serverPort}/ws"
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") }
            ?: run {
                AppLogger.e("ConnectionManager", "No token available, skipping connection")
                logger.logNetworkError("No token available, skipping connection")
                return
            }
        AppLogger.d("ConnectionManager", "Connecting to WebSocket: $url")
        diagnosticsRepository.clearManualNetworkFailure()
        logger.logInfo("Opening websocket", mapOf("url" to url))
        webSocketClient.connect(url, headers)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    /**
     * Se connecter à un serveur appairé
     * @param server Serveur appairé
     * @param handshakeSecret Secret pour le handshake (optionnel, utilise celui du serveur si disponible)
     */
    suspend fun connectToPairedServer(server: PairedServer, handshakeSecret: String? = null) {
        AppLogger.d("ConnectionManager", "Connecting to paired server: ${server.serverName} (${server.serverId})")

        val certificatePinner = server.fingerprint?.takeIf { it.isNotBlank() }?.let { fingerprint ->
            CertificatePinner.Builder()
                .add(server.host, "sha256/$fingerprint")
                .build()
        }

        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .apply { certificatePinner?.let { certificatePinner(it) } }
            .build()

        webSocketClient.updateClient(client)
        webSocketClient.updateConfig(WebSocketConfig(heartbeatIntervalMs = settings.value.heartbeatIntervalMs.toLong()))

        val savedSecret = handshakeSecret?.takeIf { it.isNotBlank() }
            ?: authRepository.getHandshakeSecret()
            ?: "change-me"

        val existingToken = authRepository.getToken()
        val token = existingToken ?: savedSecret.let { secret ->
            AppLogger.d("ConnectionManager", "Attempting handshake to ${server.baseUrl}")
            runCatching {
                authRepository.performHandshake(
                    server.baseUrl,
                    secret,
                    certificatePinner
                )
            }.onFailure { error ->
                AppLogger.e("ConnectionManager", "Handshake failed: ${error.message}", error)
                logger.logNetworkError("Handshake failed", error)
                diagnosticsRepository.recordNetworkFailure("Handshake failed: ${error.message}", error)
            }.onSuccess { token ->
                AppLogger.d("ConnectionManager", "Handshake successful, token received")
            }.getOrNull()
        }

        val url = server.webSocketUrl
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") }
            ?: run {
                AppLogger.e("ConnectionManager", "No token available, skipping connection")
                logger.logNetworkError("No token available, skipping connection")
                return
            }

        AppLogger.d("ConnectionManager", "Connecting to WebSocket: $url")
        diagnosticsRepository.clearManualNetworkFailure()
        logger.logInfo("Opening websocket to paired server", mapOf(
            "url" to url,
            "serverId" to server.serverId,
            "serverName" to server.serverName
        ))
        webSocketClient.connect(url, headers)
    }

    /**
     * Se connecter à un serveur découvert (nécessite un pairing préalable)
     * @param server Serveur découvert
     * @param handshakeSecret Secret pour le handshake
     */
    suspend fun connectToDiscoveredServer(server: DiscoveredServer, handshakeSecret: String? = null) {
        AppLogger.d("ConnectionManager", "Connecting to discovered server: ${server.serverName} (${server.serverId})")

        // Créer un PairedServer temporaire pour la connexion
        val tempPairedServer = PairedServer(
            serverId = server.serverId,
            serverName = server.serverName,
            host = server.host,
            port = server.port,
            protocol = server.protocol,
            fingerprint = null, // Pas de fingerprint pour les serveurs non appairés
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
    suspend fun attemptAutoReconnect(pairedServersRepository: com.androidcontroldeck.data.preferences.PairedServersRepository): Boolean {
        // Vérifier l'état actuel - ne pas tenter de reconnexion si déjà connecté ou en cours de connexion
        val currentState = state.value
        if (currentState is ConnectionState.Connected || currentState is ConnectionState.Connecting) {
            AppLogger.d("ConnectionManager", "Skipping auto-reconnect: already connected or connecting")
            return false
        }

        val lastServer = pairedServersRepository.getLastConnectedServer()

        if (lastServer != null && lastServer.isAutoConnect) {
            AppLogger.d("ConnectionManager", "Attempting auto-reconnect to: ${lastServer.serverName} (${lastServer.serverId})")
            logger.logInfo("Auto-reconnect attempt", mapOf(
                "serverId" to lastServer.serverId,
                "serverName" to lastServer.serverName,
                "host" to "${lastServer.host}:${lastServer.port}"
            ))

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
