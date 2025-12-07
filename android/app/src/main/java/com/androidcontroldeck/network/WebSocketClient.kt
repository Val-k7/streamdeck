package com.androidcontroldeck.network

import com.androidcontroldeck.logging.NetworkFailure
import com.androidcontroldeck.logging.UnifiedLogger
import android.os.SystemClock
import com.androidcontroldeck.network.model.AckPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import kotlin.math.pow

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
    data class Connected(val url: String) : ConnectionState()
    data class Connecting(val url: String) : ConnectionState()
    data class Disconnected(val reason: String? = null) : ConnectionState()
}

class WebSocketClient(
    okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    config: WebSocketConfig = WebSocketConfig(),
    private val networkStatusMonitor: NetworkStatusMonitor? = null,
    private val logger: UnifiedLogger? = null,
) {
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0
    private val json = Json { ignoreUnknownKeys = true }
    private data class TimedMessage(val sentAt: Long, val timerId: String? = null)

    private val inFlightMessages = mutableMapOf<String, TimedMessage>()
    private val inFlightMutex = Mutex()

    private var okHttpClient: OkHttpClient = okHttpClient
    private var config: WebSocketConfig = config

    private val _metrics = MutableStateFlow(WebSocketMetrics())
    val metrics: StateFlow<WebSocketMetrics> = _metrics.asStateFlow()

    private val _ackEvents = MutableSharedFlow<AckPayload>(extraBufferCapacity = 32)
    val ackEvents: SharedFlow<AckPayload> = _ackEvents

    // Événements pour les ACK de profil (profile:select:ack, profile:update:ack, etc.)
    private val _profileAckEvents = MutableSharedFlow<AckPayload>(extraBufferCapacity = 16)
    val profileAckEvents: SharedFlow<AckPayload> = _profileAckEvents

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _lastFailure = MutableStateFlow<NetworkFailure?>(null)
    val lastFailure: StateFlow<NetworkFailure?> = _lastFailure.asStateFlow()

    private var lastUrl: String? = null
    private var lastHeaders: Map<String, String> = emptyMap()

    init {
        networkStatusMonitor?.let { monitor ->
            scope.launch {
                monitor.isOnline.collect { online ->
                    if (online && _state.value is ConnectionState.Disconnected && lastUrl != null) {
                        reconnectAttempts = 0
                        reconnect()
                    } else if (!online) {
                        disconnect("network unavailable")
                    }
                }
            }
        }
    }

    fun connect(url: String, headers: Map<String, String> = emptyMap()) {
        lastUrl = url
        lastHeaders = headers
        if (networkStatusMonitor?.isOnline?.value == false) {
            _state.value = ConnectionState.Disconnected("network unavailable")
            markFailure("network unavailable")
            return
        }
        _state.value = ConnectionState.Connecting(url)
        logger?.logInfo("Connecting", mapOf("url" to url))
        val requestBuilder = Request.Builder().url(url)
        // Ne pas ajouter Sec-WebSocket-Extensions manuellement, OkHttp le gère automatiquement
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
        val request = requestBuilder.build()
        // Fermer proprement la connexion existante avant d'en créer une nouvelle
        try {
            webSocket?.close(1000, "Reconnecting")
        } catch (e: Exception) {
            // Ignorer les erreurs lors de la fermeture
        }
        webSocket = null
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect(reason: String? = null) {
        heartbeatJob?.cancel()
        webSocket?.close(1000, reason)
        webSocket = null
        _state.value = ConnectionState.Disconnected(reason)
        logger?.logInfo("Disconnected", mapOf("reason" to (reason ?: "user requested")))
    }

    fun updateClient(client: OkHttpClient) {
        okHttpClient = client
    }

    fun updateConfig(newConfig: WebSocketConfig) {
        config = newConfig
    }

    fun send(payload: String, messageId: String? = null) {
        if (webSocket == null && lastUrl != null) {
            reconnect()
        }
        if (_state.value is ConnectionState.Connected) {
            webSocket?.send(payload)
            if (messageId != null) {
                trackInFlight(messageId)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(config.heartbeatIntervalMs)
                if (_state.value is ConnectionState.Connected) {
                    webSocket?.send(config.heartbeatPayload)
                    val now = SystemClock.elapsedRealtime()
                    _metrics.value = _metrics.value.copy(
                        heartbeatCount = _metrics.value.heartbeatCount + 1,
                        lastHeartbeatAtMs = now,
                    )
                }
            }
        }
    }

    private fun reconnect() {
        val targetUrl = lastUrl ?: return

        // Ne pas reconnecter si le réseau n'est pas disponible
        if (networkStatusMonitor?.isOnline?.value == false) {
            logger?.logInfo("Skipping reconnect - network unavailable")
            return
        }

        // Calculer le délai avec backoff exponentiel
        val backoff = config.reconnectBaseDelayMs * 2.0.pow(reconnectAttempts.toDouble())
        val delayMs = backoff.coerceAtMost(config.reconnectMaxDelayMs.toDouble()).toLong()

        scope.launch {
            delay(delayMs)

            // Vérifier à nouveau l'état avant de reconnecter
            if (_state.value is ConnectionState.Connected) {
                logger?.logInfo("Already connected, skipping reconnect")
                return@launch
            }

            reconnectAttempts++
            _metrics.value = _metrics.value.copy(
                reconnectCount = _metrics.value.reconnectCount + 1,
                lastReconnectDelayMs = delayMs,
            )
            logger?.logInfo("Reconnecting", mapOf("url" to targetUrl, "delayMs" to delayMs, "attempt" to reconnectAttempts))
            connect(targetUrl, lastHeaders)
        }
    }

    /**
     * Réinitialise le compteur de tentatives de reconnexion (utile après une connexion réussie manuelle)
     */
    fun resetReconnectAttempts() {
        reconnectAttempts = 0
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _state.value = ConnectionState.Connected(response.request.url.toString())
            _lastFailure.value = null
            _metrics.value = _metrics.value.copy(
                reconnectCount = 0, // Réinitialiser le compteur après une connexion réussie
                lastReconnectDelayMs = null
            )
            logger?.logInfo("WebSocket open", mapOf("url" to response.request.url.toString()))
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
                        scope.launch {
                            _profileAckEvents.emit(ack)
                        }
                        logger?.logDebug("Profile ACK received", mapOf("type" to ack.type, "status" to ack.status, "messageId" to ack.messageId))
                    }
                }
            }.onFailure { logger?.logError("Unable to decode message", it) }
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
                return
            }

            // Tentative de reconnexion automatique pour les autres cas
            if (lastUrl != null && reconnectAttempts < config.maxReconnectAttempts) {
                reconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val failureReason = t.message ?: "unknown error"
            logger?.logError("WebSocket failure", t, mapOf("response" to (response?.code?.toString() ?: "null")))
            markFailure(failureReason)

            // Ne pas reconnecter si c'est une erreur d'authentification
            val shouldReconnect = response?.code != 401 && response?.code != 403
            if (shouldReconnect && lastUrl != null && reconnectAttempts < config.maxReconnectAttempts) {
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
        _lastFailure.value = NetworkFailure(
            message = reason,
            at = System.currentTimeMillis()
        )
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
                _metrics.value = current.copy(
                    ackCount = totalAcks,
                    averageAckLatencyMs = average,
                    lastAckLatencyMs = lat,
                )
                _ackEvents.emit(ack)
            } ?: run {
                // Message non trouvé dans inFlight - probablement timeout
                _metrics.value = _metrics.value.copy(droppedAcks = _metrics.value.droppedAcks + 1)
            }
        }
    }

    private fun trackInFlight(messageId: String) {
        scope.launch {
            val timerId = logger?.startTimedEvent(
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
                    _metrics.value = _metrics.value.copy(
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
