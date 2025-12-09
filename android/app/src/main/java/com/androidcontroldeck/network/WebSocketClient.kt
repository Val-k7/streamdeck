package com.androidcontroldeck.network

import com.androidcontroldeck.logging.UnifiedLogger
import com.androidcontroldeck.network.model.AckPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WebSocketConfig(
        val heartbeatIntervalMs: Long = 15_000,
        val heartbeatPayload: String = "\uD83D\uDCBE",
        val reconnectBaseDelayMs: Long = 1_000,
        val reconnectMaxDelayMs: Long = 30_000,
        val maxReconnectAttempts: Int = 6,
        val ackTimeoutMs: Long = 5_000,
)

data class WebSocketMetrics(
        val heartbeatCount: Long = 0,
        val reconnectCount: Long = 0,
        val lastHeartbeatAtMs: Long? = null,
        val lastReconnectDelayMs: Long? = null,
        val ackCount: Long = 0,
        val lastAckLatencyMs: Long? = null,
        val averageAckLatencyMs: Double = 0.0,
        val droppedAcks: Long = 0,
        val inFlightMessages: Int = 0,
)

sealed class ConnectionState {
    data class Connected(val url: String = "") : ConnectionState()
    data class Connecting(val url: String = "") : ConnectionState()
    data class Disconnected(val reason: String? = null) : ConnectionState()
}

/**
 * Stub WebSocket client for simplified Android WebView build. No network operations are performed.
 */
class WebSocketClient(
        private val scope: Any? = null,
        private val networkStatusMonitor: Any? = null,
        private val logger: UnifiedLogger? = null,
) {
    private val _metrics = MutableStateFlow(WebSocketMetrics())
    val metrics: StateFlow<WebSocketMetrics> = _metrics.asStateFlow()

    private val _ackEvents = MutableSharedFlow<AckPayload>(extraBufferCapacity = 8)
    val ackEvents: SharedFlow<AckPayload> = _ackEvents

    private val _profileAckEvents = MutableSharedFlow<AckPayload>(extraBufferCapacity = 8)
    val profileAckEvents: SharedFlow<AckPayload> = _profileAckEvents

    private val _authFailureEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val authFailureEvents: SharedFlow<String> = _authFailureEvents

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun connect(url: String, headers: Map<String, String> = emptyMap()) {
        logger?.logInfo("StubWebSocketClient connect", mapOf("url" to url, "headers" to headers))
        _state.value = ConnectionState.Connected(url)
    }

    fun disconnect(reason: String? = null) {
        logger?.logInfo("StubWebSocketClient disconnect", mapOf("reason" to (reason ?: "")))
        _state.value = ConnectionState.Disconnected(reason)
    }

    fun updateClient(client: Any?) {
        logger?.logDebug(
                "StubWebSocketClient updateClient",
                mapOf("client" to (client?.toString() ?: "null"))
        )
    }

    fun updateConfig(newConfig: WebSocketConfig) {
        logger?.logDebug(
                "StubWebSocketClient updateConfig",
                mapOf("config" to newConfig.toString())
        )
    }

    fun send(payload: String, messageId: String? = null) {
        logger?.logDebug(
                "StubWebSocketClient send",
                mapOf("payload" to payload, "messageId" to (messageId ?: ""))
        )
    }

    /**
     * Réinitialise le compteur de tentatives de reconnexion (utile après une connexion réussie
     * manuelle)
     */
    fun resetReconnectAttempts() {
        reconnectAttempts = 0
    }

    private val listener =
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempts = 0
                    _state.value = ConnectionState.Connected(response.request.url.toString())
                    _lastFailure.value = null
                    _metrics.value =
                            _metrics.value.copy(
                                    reconnectCount =
                                            0, // Réinitialiser le compteur après une connexion
                                    // réussie
                                    lastReconnectDelayMs = null
                            )
                    logger?.logInfo(
                            "WebSocket open",
                            mapOf("url" to response.request.url.toString())
                    )
                    startHeartbeat()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    logger?.logDebug("message", mapOf("payload" to text))
                    runCatching {
                        val ack = json.decodeFromString<AckPayload>(text)
                        when {
                            ack.type == "ack" -> handleAck(ack)
                            ack.type.startsWith("profile:") && ack.type.endsWith(":ack") -> {
                                // ACK de profil (profile:select:ack, profile:update:ack, etc.)
                                scope.launch { _profileAckEvents.emit(ack) }
                                logger?.logDebug(
                                        "Profile ACK received",
                                        mapOf(
                                                "type" to ack.type,
                                                "status" to ack.status,
                                                "messageId" to ack.messageId
                                        )
                                )
                            }
                        }
                    }
                            .onFailure { logger?.logError("Unable to decode message", it) }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    logger?.logDebug("message bytes", mapOf("payload" to bytes.utf8()))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    _state.value = ConnectionState.Disconnected("closing: $code $reason")
                    logger?.logInfo("Closing", mapOf("code" to code, "reason" to reason))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _state.value = ConnectionState.Disconnected("closed: $code $reason")
                    logger?.logInfo("Closed", mapOf("code" to code, "reason" to reason))

                    // Ne pas reconnecter automatiquement si la fermeture est normale (code 1000)
                    // ou si c'est une erreur d'authentification
                    if (code == 1000 || code == 4001) {
                        // Émettre un événement d'échec d'authentification pour que le
                        // ConnectionManager puisse nettoyer le token
                        if (code == 4001) {
                            scope.launch { _authFailureEvents.emit(reason) }
                        }
                        return
                    }

                    // Tentative de reconnexion automatique pour les autres cas
                    if (lastUrl != null && reconnectAttempts < config.maxReconnectAttempts) {
                        reconnect()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val failureReason = t.message ?: "unknown error"
                    logger?.logError(
                            "WebSocket failure",
                            t,
                            mapOf("response" to (response?.code?.toString() ?: "null"))
                    )
                    markFailure(failureReason)

                    // Ne pas reconnecter si c'est une erreur d'authentification
                    val shouldReconnect = response?.code != 401 && response?.code != 403
                    if (shouldReconnect &&
                                    lastUrl != null &&
                                    reconnectAttempts < config.maxReconnectAttempts
                    ) {
                        _state.value = ConnectionState.Disconnected(failureReason)
                        reconnect()
                    } else {
                        _state.value = ConnectionState.Disconnected(failureReason)
                        if (reconnectAttempts >= config.maxReconnectAttempts) {
                            logger?.logError("Max reconnection attempts reached", null)
                        }
                    }
                }
            }

    private fun markFailure(reason: String) {
        _lastFailure.value = NetworkFailure(message = reason, at = System.currentTimeMillis())
    }

    private fun handleAck(ack: AckPayload) {
        scope.launch {
            var latency: Long? = null
            var timerId: String? = null
            inFlightMutex.withLock {
                val message = inFlightMessages.remove(ack.messageId)
                if (message != null) {
                    latency = SystemClock.elapsedRealtime() - message.sentAt
                    timerId = message.timerId
                    _metrics.value = _metrics.value.copy(inFlightMessages = inFlightMessages.size)
                }
            }
            latency?.let { lat ->
                timerId?.let { id ->
                    logger?.endTimedEvent(
                            id = id,
                            name = "control_event",
                            metadata = mapOf("latencyMs" to lat, "messageId" to ack.messageId)
                    )
                }
                // Mettre à jour les métriques
                val current = _metrics.value
                val totalAcks = current.ackCount + 1
                val average = ((current.averageAckLatencyMs * current.ackCount) + lat) / totalAcks
                _metrics.value =
                        current.copy(
                                ackCount = totalAcks,
                                averageAckLatencyMs = average,
                                lastAckLatencyMs = lat,
                        )
                _ackEvents.emit(ack)
            }
                    ?: run {
                        // Message non trouvé dans inFlight - probablement timeout
                        _metrics.value =
                                _metrics.value.copy(droppedAcks = _metrics.value.droppedAcks + 1)
                    }
        }
    }

    private fun trackInFlight(messageId: String) {
        scope.launch {
            val timerId =
                    logger?.startTimedEvent(
                            name = "control_event",
                            metadata = mapOf("messageId" to messageId)
                    )
            inFlightMutex.withLock {
                inFlightMessages[messageId] = TimedMessage(SystemClock.elapsedRealtime(), timerId)
                _metrics.value = _metrics.value.copy(inFlightMessages = inFlightMessages.size)
            }
            delay(config.ackTimeoutMs)
            inFlightMutex.withLock {
                val timed = inFlightMessages.remove(messageId)
                if (timed != null) {
                    _metrics.value =
                            _metrics.value.copy(
                                    droppedAcks = _metrics.value.droppedAcks + 1,
                                    inFlightMessages = inFlightMessages.size,
                            )
                    timed.timerId?.let { timer ->
                        logger?.endTimedEvent(
                                id = timer,
                                name = "control_event",
                                metadata = mapOf("status" to "timeout", "messageId" to messageId)
                        )
                    }
                }
            }
        }
    }
}
