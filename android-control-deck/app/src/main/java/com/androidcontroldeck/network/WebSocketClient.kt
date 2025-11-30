package com.androidcontroldeck.network

import android.os.SystemClock
import android.util.Log
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
) {
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0
    private val json = Json { ignoreUnknownKeys = true }
    private val inFlightMessages = mutableMapOf<String, Long>()
    private val inFlightMutex = Mutex()

    private var okHttpClient: OkHttpClient = okHttpClient
    private var config: WebSocketConfig = config

    private val _metrics = MutableStateFlow(WebSocketMetrics())
    val metrics: StateFlow<WebSocketMetrics> = _metrics.asStateFlow()

    private val _ackEvents = MutableSharedFlow<AckPayload>(extraBufferCapacity = 32)
    val ackEvents: SharedFlow<AckPayload> = _ackEvents

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

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
            return
        }
        _state.value = ConnectionState.Connecting(url)
        val requestBuilder = Request.Builder().url(url)
            .header("Sec-WebSocket-Extensions", "permessage-deflate")
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
        val request = requestBuilder.build()
        webSocket?.cancel()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect(reason: String? = null) {
        heartbeatJob?.cancel()
        webSocket?.close(1000, reason)
        webSocket = null
        _state.value = ConnectionState.Disconnected(reason)
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
        val backoff = config.reconnectBaseDelayMs * 2.0.pow(reconnectAttempts.toDouble())
        val delayMs = backoff.coerceAtMost(config.reconnectMaxDelayMs.toDouble()).toLong()
        scope.launch {
            delay(delayMs)
            reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(config.maxReconnectAttempts)
            _metrics.value = _metrics.value.copy(
                reconnectCount = _metrics.value.reconnectCount + 1,
                lastReconnectDelayMs = delayMs,
            )
            connect(targetUrl, lastHeaders)
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _state.value = ConnectionState.Connected(response.request.url.toString())
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocketClient", "message: $text")
            runCatching {
                val ack = json.decodeFromString<AckPayload>(text)
                if (ack.type == "ack") handleAck(ack)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d("WebSocketClient", "message bytes: ${bytes.utf8()}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = ConnectionState.Disconnected("closing: $code $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = ConnectionState.Disconnected("closed: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = ConnectionState.Disconnected(t.message)
            reconnect()
        }
    }

    private suspend fun updateAckMetrics(latency: Long) {
        val current = _metrics.value
        val totalAcks = current.ackCount + 1
        val average = ((current.averageAckLatencyMs * current.ackCount) + latency) / totalAcks
        _metrics.value = current.copy(
            ackCount = totalAcks,
            averageAckLatencyMs = average,
            lastAckLatencyMs = latency,
        )
    }

    private fun handleAck(ack: AckPayload) {
        scope.launch {
            var latency: Long? = null
            inFlightMutex.withLock {
                val sentAt = inFlightMessages.remove(ack.messageId)
                if (sentAt != null) {
                    latency = SystemClock.elapsedRealtime() - sentAt
                    _metrics.value = _metrics.value.copy(inFlightMessages = inFlightMessages.size)
                }
            }
            latency?.let { updateAckMetrics(it) }
            _ackEvents.tryEmit(ack)
        }
    }

    private fun trackInFlight(messageId: String) {
        scope.launch {
            inFlightMutex.withLock {
                inFlightMessages[messageId] = SystemClock.elapsedRealtime()
                _metrics.value = _metrics.value.copy(inFlightMessages = inFlightMessages.size)
            }
            delay(config.ackTimeoutMs)
            inFlightMutex.withLock {
                if (inFlightMessages.remove(messageId) != null) {
                    _metrics.value = _metrics.value.copy(
                        droppedAcks = _metrics.value.droppedAcks + 1,
                        inFlightMessages = inFlightMessages.size,
                    )
                }
            }
        }
    }
}
